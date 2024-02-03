package io.continual.notify;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.data.StreamTools;
import io.continual.util.data.exprEval.EnvDataSource;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.time.Clock;

/**
 * A minimal client for sending notifications to Continual's Rcvr system.
 */
public class ContinualNotifier
{
	/**
	 * Send an alert to Continual using credentials and topic/stream data from the environment.
	 * @param subject
	 * @param condition
	 */
	public static void send ( String subject, String condition )
	{
		new ContinualNotifier()
			.onSubject ( subject )
			.withCondition ( condition )
			.send ()
		;
	}

	public ContinualNotifier ()
	{
		fUser = evalSetting ( "CONTINUAL_USER" );
		fPassword = evalSetting ( "CONTINUAL_PASSWORD" );

		fTopic = evalSetting ( "CONTINUAL_RCVR_TOPIC" );
		fSourceSystem = evalSetting ( "CONTINUAL_SYSTEM" );
		if ( fSourceSystem != null && fSourceSystem.length () > 0 )
		{
			onSubject ( fSourceSystem );
		}

		fStream = evalSetting ( "CONTINUAL_RCVR_STREAM" );
		if ( fStream == null && fSourceSystem != null && fSourceSystem.length () > 0 )
		{
			fStream = fSourceSystem;
		}

		fMsg.put ( kVersionTag, kVersion );
	}

	public ContinualNotifier toTopic ( String topic )
	{
		fTopic = topic;
		return this;
	}

	public ContinualNotifier onStream ( String stream )
	{
		fStream = stream;
		return this;
	}

	public ContinualNotifier asUser ( String user, String pwd )
	{
		if ( user == null || user.length () == 0 ) throw new IllegalArgumentException ( "Provide a username." );
		if ( pwd == null || pwd.length () == 0 ) throw new IllegalArgumentException ( "Provide a password." );

		fUser = user;
		fPassword = pwd;
		return this;
	}

	public ContinualNotifier inForeground ()
	{
		fBackground = false;
		return this;
	}

	public ContinualNotifier inBackground ()
	{
		fBackground = true;
		return this;
	}

	public ContinualNotifier onSubject ( String subject )
	{
		if ( subject == null ) throw new IllegalArgumentException ( "Provide a subject." );
		fMsg.put ( kSubject, subject );
		return this;
	}
	
	public ContinualNotifier withCondition ( String condition )
	{
		if ( condition == null ) throw new IllegalArgumentException ( "Provide a subject." );
		fMsg.put ( kCondition, condition );
		return this;
	}

	public ContinualNotifier asOnset ()
	{
		fMsg.put ( kOnset, true );
		return this;
	}

	public ContinualNotifier asClear ()
	{
		fMsg.put ( kOnset, false );
		return this;
	}

	public ContinualNotifier withDetails ( String details )
	{
		if ( details == null ) throw new IllegalArgumentException ( "Provide details." );
		fMsg.put ( kDetails, details);
		return this;
	}

	public ContinualNotifier withAddlData ( String key, Object val )
	{
		fMsg.put ( key, val );
		return this;
	}

	public void send ()
	{
		withAddlData ( kQueuedAt, Clock.now () );

		final Sender mySender = new Sender ();
		if ( fBackground )
		{
			skBackgroundSender.submit ( mySender );
		}
		else
		{
			mySender.run ();
		}
	}

	private String fTopic = null;
	private String fStream = null;
	private JSONObject fMsg = new JSONObject ();
	private String fUser = null;
	private String fPassword = null;
	private boolean fBackground = true;
	private String fSourceSystem = null;

	private static final String kVersionTag = "ver";
	private static final String kVersion = "1.0";

	private static final String kSubject = "subj";
	private static final String kCondition = "cond";
	private static final String kDetails = "details";

	private static final String kOnset = "onset";

	private static final String kQueuedAt = "queuedAt";
	private static final String kSendAt = "sentAt";

	private class Sender implements Runnable
	{
		@Override
		public void run ()
		{
			if ( fUser == null || fPassword == null )
			{
				warn ( "Skipping message send because credentials are not set." );
				return;
			}

			try
			{
				// generate the URL
				String urlText = skBaseUrlStr;
				if ( fTopic != null )
				{
					urlText = urlText + "/" + URLEncoder.encode ( fTopic, StandardCharsets.UTF_8.toString () );
					if  ( fStream != null )
					{
						urlText = urlText + "/" + URLEncoder.encode ( fStream, StandardCharsets.UTF_8.toString () );
					}
				}

				final URL url = new URL ( urlText );
				final byte[] payload = fMsg
					.put ( kSendAt, Clock.now () )
					.toString ()
					.getBytes ( StandardCharsets.UTF_8 )
				;

				final HttpURLConnection conn = (HttpURLConnection) url.openConnection ();

				// posting JSON data
				conn.setRequestMethod ( "POST" );
				conn.setRequestProperty ( "Content-Type", "application/json" );

				// setup a timeout
				conn.setConnectTimeout ( 15000 );

				// basic auth
				final String authString = fUser + ":" + fPassword;
				final String encodedAuthString = Base64.getEncoder ().encodeToString ( authString.getBytes ( StandardCharsets.UTF_8 ) );
				conn.setRequestProperty ( "Authorization", "Basic " + encodedAuthString );

				// setup the streams 
				conn.setDoInput ( true );
				conn.setDoOutput ( true );

				// write our data
				conn.setRequestProperty ( "Content-Length", String.valueOf ( payload.length ) );
				conn.getOutputStream ().write ( payload );

				// read the response 
				final int responseCode = conn.getResponseCode ();
				final byte[] response = StreamTools.readBytes ( conn.getInputStream () );
				if ( HttpStatusCodes.isSuccess ( responseCode ) )
				{
					debug ( "ok: " + responseCode + " " + new String ( response ).trim () );
				}
				else
				{
					warn ( "failed: " + responseCode );
				}

				// Close the connection
				conn.disconnect ();
			}
			catch ( IOException e )
			{
				warn ( "failed with exception: " + e.getMessage () );
			}
		}
	}
	
	private static final String skBaseUrlStr = "https://rcvr.continual.io/events";
	private static final ExpressionEvaluator skEval = new ExpressionEvaluator ( new EnvDataSource () );

	private static final Logger log = LoggerFactory.getLogger ( ContinualNotifier.class );

	private static ExecutorService skBackgroundSender = Executors.newSingleThreadExecutor();

	private static String formatLog ( String msg )
	{
		return "Continual.Notifier " + msg;
	}

	private static void debug ( String msg )
	{
		log.info ( formatLog ( msg ) );
	}
	
	private static void warn ( String msg )
	{
		log.warn ( formatLog ( msg ) );
	}

	private static String evalSetting ( String key )
	{
		final String val = skEval.evaluateText ( "${" + key + "}" );
		return val == null || val.length () == 0 ? null : val;
	}
}
