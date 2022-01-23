package io.continual.services.processor.library.email.sources;

import java.io.IOException;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.email.impl.SimpleEmailService;
import io.continual.services.ServiceContainer;
import io.continual.services.processor.engine.library.sources.BasicSource;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.data.exprEval.ExpressionEvaluator;

public class EmailInboxReader extends BasicSource
{
	public EmailInboxReader ( ServiceContainer sc, JSONObject config )
	{
		final ExpressionEvaluator ee = sc.getExprEval ( config );
		
		fHost = ee.evaluateText ( config.optString ( kSetting_ImapServer, "imap.gmail.com" ) );
		fUser = ee.evaluateText ( config.optString ( kSetting_MailLogin, null ) );
		fPassword = ee.evaluateText ( config.optString ( kSetting_MailPassword, null ) );
		fFolder = ee.evaluateText ( config.optString ( kSetting_MailFolder, "inbox" ) );
		
		fMailProps = new Properties ();
		fMailProps.put ( "mail.imaps.host", fHost );
		fMailProps.put ( "mail.imaps.port", "" + ee.evaluateTextToInt ( config.opt ( kSetting_ImapServerPort ), 993 ) );
		fMailProps.put ( "mail.pop3.starttls.enable", "" + ee.evaluateTextToBoolean ( config.opt ( kSetting_ImapServerSsl ), true ) );
//		fMailProps.put ( "mail.smtp.socketFactory.fallback", "false" );
//		fMailProps.put ( "mail.smtp.quitwait", "false" );
//		fMailProps.put ( "mail.smtp.auth", "" + ee.evaluateTextToBoolean ( config.opt ( kSetting_SmtpServerUseAuth ), true ) ); 
	}

	@Override
	protected MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc ) throws IOException, InterruptedException
	{
		readNextBatch ();
		
		noteEndOfStream ();
		return null;
	}

	private final Properties fMailProps;
	private final String fHost;
	private final String fUser;
	private final String fPassword;
	private final String fFolder;

	public static final String kSetting_MailLogin = SimpleEmailService.kSetting_MailLogin;
	public static final String kSetting_MailPassword = SimpleEmailService.kSetting_MailPassword;
	public static final String kSetting_MailFolder = "folder";

	public static final String kSetting_MailFromEmail = SimpleEmailService.kSetting_MailFromEmail;
	public static final String kSetting_MailFromName = SimpleEmailService.kSetting_MailFromName;

	public static final String kSetting_Pop3Server = "mailPop3Server";
	public static final String kSetting_Pop3ServerPort = "mailPop3ServerPort";
	public static final String kSetting_Pop3ServerSsl = "mailPop3ServerSsl";
	public static final String kSetting_Pop3ServerUseAuth = "mailPop3ServerUseAuth";

	public static final String kSetting_ImapServer = "mailPop3Server";
	public static final String kSetting_ImapServerPort = "mailPop3ServerPort";
	public static final String kSetting_ImapServerSsl = "mailPop3ServerSsl";
	public static final String kSetting_ImapServerUseAuth = "mailPop3ServerUseAuth";

	private void readNextBatch ()
	{
		try
		{
			final Session session = Session.getDefaultInstance ( fMailProps );

			try ( final Store store = session.getStore ( "imaps" ) )
			{
				store.connect ( fHost, fUser, fPassword );	// yes, we have to provide it twice
	
				try ( final Folder emailFolder = store.getFolder ( fFolder ) )
				{
					emailFolder.open ( Folder.READ_ONLY );

					final Message[] msgs = emailFolder.getMessages ();

//					final Message[] msgs = emailFolder.search ( new SearchTerm ()
//					{
//						@Override
//						public boolean match ( Message msg )
//						{
//							try
//							{
//								final Flags flags = msg.getFlags ();
//								return flags.contains ( Flag.RECENT );
//							}
//							catch ( MessagingException x )
//							{
//								return false;
//							}
//						}
//						private static final long serialVersionUID = 1L;
//					} );

					for ( Message msg : msgs )
					{
						System.out.println ( "---------------------------------" );
						System.out.println ( "Subject: " + msg.getSubject () );
						System.out.println ( "From: " + msg.getFrom ()[0] );

						final String contentType = msg.getContentType ();
						System.out.println ( "Type: " + contentType );
						
//						if ( msg.isMimeType ( MimeTypes.kMultipart ) )
//						{
//							final MimeMultipart mimeMultipart = (MimeMultipart) msg.getContent ();
//							final int count = mimeMultipart.getCount ();
//							System.out.println ( "Parts: " + count );
//						}
					}
					
//					System.out.println ( "messages.length---" + messages.length );
//
//					for ( int i = 0, n = messages.length; i < n; i++ )
//					{
//						Message message = messages[i];
//						System.out.println ( "---------------------------------" );
//						System.out.println ( "Email Number " + ( i + 1 ) );
//						System.out.println ( "Subject: " + message.getSubject () );
//						System.out.println ( "From: " + message.getFrom ()[0] );
//		//				System.out.println ( "Text: " + message.getContent ().toString () );
//					}
				}
			}

//			final Message msg = new MimeMessage ( session );
//
//			// subject
//			msg.setSubject ( fBuilder.fSubj );
//
//			// addressing
//			final InternetAddress from = new InternetAddress ( fFromAddr, fFromName );
//			msg.setFrom ( from );
//			msg.setReplyTo ( new InternetAddress[] { from } );
//			for ( String toAddr : fBuilder.fTos )
//			{
//				final InternetAddress to = new InternetAddress ( toAddr );
//				msg.addRecipient ( Message.RecipientType.TO, to );
//			}
//
//			if ( fBuilder.isMultipart () )
//			{
//				// message content alternatives
//				final Multipart contentAlternatives = new MimeMultipart ( "alternative" );
//				for ( MimeBodyPart bodyPart : fBuilder.fParts )
//				{
//					contentAlternatives.addBodyPart ( bodyPart );
//				}
//
//				// top-level...
//				final MimeBodyPart altsBodyPart = new MimeBodyPart ();
//				altsBodyPart.setContent ( contentAlternatives );
//
//				final Multipart mixedMultipart = new MimeMultipart ( "mixed" );
//				mixedMultipart.addBodyPart ( altsBodyPart );
//				msg.setContent ( mixedMultipart );
//			}
//			else
//			{
//				msg.setText ( fBuilder.getText () );
//			}
//				
//			final Transport transport = session.getTransport ( "smtp" );
//			transport.connect ( fUser, fPassword );
//			transport.sendMessage ( msg, msg.getAllRecipients () );
//			transport.close ();
//
//			log.info ( "Mail sent." );
//
//			return new SimpleMailStatus ();
		}
		catch ( MessagingException e ) //| UnsupportedEncodingException e )
		{
			log.warn ( "Error reading email: {}", e.getMessage(), e );
//			return new SimpleMailStatus ( e.getMessage () );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( EmailInboxReader.class );
}
