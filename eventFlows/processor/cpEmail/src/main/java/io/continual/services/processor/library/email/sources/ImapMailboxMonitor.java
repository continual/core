package io.continual.services.processor.library.email.sources;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeMessage;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.email.impl.SimpleEmailService;
import io.continual.services.ServiceContainer;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.library.sources.BasicSource;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.services.processor.library.email.sources.support.DataLoader;
import io.continual.services.processor.library.email.sources.support.SeenTracker;
import io.continual.services.processor.library.email.sources.support.SimpleMessageDataLoader;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.time.Clock;

public class ImapMailboxMonitor extends BasicSource
{
	public ImapMailboxMonitor ( ConfigLoadContext clc, JSONObject config ) throws JSONException, BuildFailure
	{
		this ( clc.getServiceContainer (), config );
	}

	public ImapMailboxMonitor ( ServiceContainer sc, JSONObject config ) throws JSONException, BuildFailure
	{
		super ();

		final ExpressionEvaluator ee = sc.getExprEval ( config );
		
		fHost = ee.evaluateText ( config.optString ( kSetting_ImapServer, "imap.gmail.com" ) );
		fUser = ee.evaluateText ( config.optString ( kSetting_MailLogin, null ) );
		fPassword = ee.evaluateText ( config.optString ( kSetting_MailPassword, null ) );
		fFolder = ee.evaluateText ( config.optString ( kSetting_MailFolder, "inbox" ) );

		fLastPollMs = 0L;
		fPollIntervalMs = 1000L * 60 * ee.evaluateTextToLong ( config.opt ( kSetting_PollFreqMinutes ), kDefault_PollFreqMinutes );
		
		fMailProps = new Properties ();
		fMailProps.put ( "mail.imaps.host", fHost );
		fMailProps.put ( "mail.imaps.port", "" + ee.evaluateTextToInt ( config.opt ( kSetting_ImapServerPort ), 993 ) );
		fMailProps.put ( "mail.pop3.starttls.enable", "" + ee.evaluateTextToBoolean ( config.opt ( kSetting_ImapServerSsl ), true ) );
		fMailProps.put ( "mail.imaps.usesocketchannels", "true" );	// required for IMAP watch 
		
		fPending = new LinkedList<>();
		fSeenTracker = Builder.fromJson ( SeenTracker.class, config.getJSONObject ( "tracker" ), sc );

		final JSONObject dataLoader = config.optJSONObject ( "dataLoader" );
		if ( dataLoader != null )
		{
			fDataLoader = Builder.fromJson ( DataLoader.class, dataLoader );
		}
		else
		{
			fDataLoader = new SimpleMessageDataLoader ();
		}
	}

	@Override
	protected MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc ) throws IOException, InterruptedException
	{
		if ( fPending.size () == 0 )
		{
			readNextBatch ();
		}
		if ( fPending.size () > 0 )
		{
			return makeDefRoutingMessage ( fPending.removeFirst () );
		}
		return null;
	}

	private final Properties fMailProps;
	private final String fHost;
	private final String fUser;
	private final String fPassword;
	private final String fFolder;

	private long fLastPollMs;
	private final long fPollIntervalMs;

	private final LinkedList<io.continual.services.processor.engine.model.Message> fPending;
	private final SeenTracker fSeenTracker;
	private final DataLoader fDataLoader;

	public static final String kSetting_MailLogin = SimpleEmailService.kSetting_MailLogin;
	public static final String kSetting_MailPassword = SimpleEmailService.kSetting_MailPassword;
	public static final String kSetting_MailFolder = "folder";
	public static final String kSetting_PollFreqMinutes = "pollEveryMins";

	public static final String kSetting_ImapServer = "mailImapServer";
	public static final String kSetting_ImapServerPort = "mailImapServerPort";
	public static final String kSetting_ImapServerSsl = "mailImapServerSsl";
	public static final String kSetting_ImapServerUseAuth = "mailImapServerUseAuth";

	public static final long kDefault_PollFreqMinutes = 5L;

	private synchronized void enqueue ( long uid, Message msg ) throws MessagingException
	{
		if ( fSeenTracker.isUidSeen ( uid ) )
		{
			log.debug ( "Msg " + uid + " was seen here; skipping" );
			return;
		}
		fSeenTracker.addUid ( uid );

		if ( msg.getFlags ().contains ( Flags.Flag.DELETED ) )
		{
			log.debug ( "Msg " + uid + " is deleted; skipping" );
			return;
		}

		log.info ( "Msg " + uid + " is new: " + msg.getSubject () + " from " + msg.getFrom ()[0].toString () );
		if ( msg instanceof MimeMessage )
		{
			try
			{
				for ( io.continual.services.processor.engine.model.Message mmw : fDataLoader.getMessages ( uid, (MimeMessage) msg ) )
				{
					fPending.add ( mmw );
				}
			}
			catch ( BuildFailure | IOException e )
			{
				log.warn ( "Failed to read message from IMAP email: " + e.getMessage () );
			}
		}
		else
		{
			// FIXME: handle these
			log.warn ( "Unhandled non-MIME msg" );
		}
	}

	private synchronized void readNextBatch () throws IOException
	{
		final long nowMs = Clock.now ();
		if ( fLastPollMs + fPollIntervalMs > nowMs )
		{
			return;
		}

		fLastPollMs = nowMs;

		try
		{
			final Session session = Session.getDefaultInstance ( fMailProps );

			try ( final Store store = session.getStore ( "imaps" ) )
			{
				store.connect ( fHost, fUser, fPassword );	// yes, we have to provide host twice
	
				try ( final Folder emailFolder = store.getFolder ( fFolder ) )
				{
					final UIDFolder uidFolder = (UIDFolder) emailFolder;

					emailFolder.open ( Folder.READ_ONLY );

					final Message[] msgs = emailFolder.getMessages ();
					for ( Message msg : msgs )
					{
						enqueue ( uidFolder.getUID ( msg ), msg );
					}
				}
				catch ( Exception e )
				{
					log.warn ( e.getMessage (), e );
				}
			}
		}
		catch ( AuthenticationFailedException e )
		{
			log.warn ( "Error reading email: {}", e.getMessage(), e );
			throw new IOException ( e );
		}
		catch ( MessagingException e )
		{
			log.warn ( "Error reading email: {}", e.getMessage(), e );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( ImapMailboxMonitor.class );
}
