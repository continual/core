package io.continual.notify;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.data.StreamTools;
import io.continual.util.data.exprEval.EnvDataSource;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.naming.Path;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.time.Clock;

/**
 * A minimal client for sending notifications to Continual's Rcvr system.
 */
public class ContinualNotifier
{
	/**
	 * Send a notification to Continual using credentials and topic/stream data from the environment.
	 * @param subject the subject of the notification
	 * @param condition the condition that's occurred with respect to the subject
	 */
	public static void send ( String subject, String condition )
	{
		new ContinualNotifier ()
			.onSubject ( subject )
			.withCondition ( condition )
			.send ()
		;
	}

	/**
	 * Construct a notifier instance based on the settings provided in the environment, including
	 * CONTINUAL_USER, CONTINUAL_PASSWORD, CONTINUAL_SYSTEM, CONTINUAL_RCVR_TOPIC, and CONTINUAL_RCVR_STREAM.
	 */
	public ContinualNotifier ()
	{
		fUser = evalSetting ( "CONTINUAL_USER" );
		fPassword = evalSetting ( "CONTINUAL_PASSWORD" );

		fTopic = evalSetting ( "CONTINUAL_RCVR_TOPIC" );
		final String sourceSystem = evalSetting("CONTINUAL_SYSTEM");
		if ( sourceSystem != null && !sourceSystem.isEmpty() )
		{
			onSubject ( sourceSystem );
		}

		fStream = evalSetting ( "CONTINUAL_RCVR_STREAM" );
		if ( fStream == null && sourceSystem != null && !sourceSystem.isEmpty() )
		{
			fStream = sourceSystem;
		}

		fMsg.put ( kVersionTag, kVersion );
	}

	/**
	 * Close the background send queue and wait for it to finish work. After this call, no further background
	 * sends will be accepted and attempts to send in the background will fail with an illegal state exception.
	 * @throws InterruptedException if the thread is interrupted while waiting for background sender shutdown
	 */
	public static void closeAndWaitForBackgroundSends ( long timeoutMs ) throws InterruptedException
	{
		skBackgroundSender.shutdown ();
		if ( !skBackgroundSender.awaitTermination ( timeoutMs, TimeUnit.MILLISECONDS ) )
		{
			log.warn ( "ContinualNotifier background sender did not stop properly." );
		}
	}
	
	/**
	 * Specify the topic to which the notification will be sent.
	 * @param topic the notification topic
	 * @return this notifier
	 */
	public ContinualNotifier toTopic ( String topic )
	{
		fTopic = topic;
		return this;
	}

	/**
	 * Specify the stream within the topic to which the notification will be sent.
	 * @param stream the notification stream
	 * @return this notifier
	 */
	public ContinualNotifier onStream ( String stream )
	{
		fStream = stream;
		return this;
	}

	/**
	 * Specify the user and password to use for sending the notification.
	 * @param user the notification service user
	 * @param pwd the notification service password
	 * @return this notifier
	 */
	public ContinualNotifier asUser ( String user, String pwd )
	{
		if ( user == null || user.isEmpty()) throw new IllegalArgumentException ( "Provide a username." );
		if ( pwd == null || pwd.isEmpty()) throw new IllegalArgumentException ( "Provide a password." );

		fUser = user;
		fPassword = pwd;
		return this;
	}

	/**
	 * Specify that the notification should be sent in the foreground, i.e., in the current thread.
	 * @return this notifier
	 */
	public ContinualNotifier inForeground ()
	{
		fBackground = false;
		return this;
	}

	/**
	 * Specify that the notification should be sent in a background thread.
	 * @return this notifier
	 */
	public ContinualNotifier inBackground ()
	{
		fBackground = true;
		return this;
	}

	/**
	 * Specify the subject of the notification.
	 * @param subject the subject of the notification
	 * @return this notifier
	 */
	public ContinualNotifier onSubject ( String subject )
	{
		if ( subject == null ) throw new IllegalArgumentException ( "Provide a subject." );
		fMsg.put ( kSubject, subject );
		return this;
	}

	/**
	 * Specify the subject of the notification.
	 * @param subject the subject of the notification
	 * @return this notifier
	 */
	public ContinualNotifier onSubject ( Path subject )
	{
		return onSubject ( subject.toString () );
	}

	/**
	 * Specify the condition that's occurred with respect to the subject.
	 * @param condition the condition that's occurred
	 * @return this notifier
	 */
	public ContinualNotifier withCondition ( String condition )
	{
		if ( condition == null ) throw new IllegalArgumentException ( "Provide a condition." );
		fMsg.put ( kCondition, condition );
		return this;
	}

	/**
	 * Specify that the notification is an onset.
	 * @return this notifier
	 */
	public ContinualNotifier asOnset ()
	{
		fMsg.put ( kOnset, true );
		return this;
	}

	/**
	 * Specify that the notification is a clear.
	 * @return this notifier
	 */
	public ContinualNotifier asClear ()
	{
		fMsg.put ( kOnset, false );
		return this;
	}

	/**
	 * Provide additional details about the notification.
	 * @param details additional details for the notification
	 * @return this notifier
	 */
	public ContinualNotifier withDetails ( String details )
	{
		if ( details == null ) throw new IllegalArgumentException ( "Provide details." );
		fMsg.put ( kDetails, details);
		return this;
	}

	/**
	 * Provide additional data for the notification.
	 * @param key the key for the additional data
	 * @param val the value for the additional data
	 * @return this notifier
	 */
	public ContinualNotifier withAddlData ( String key, Object val )
	{
		fMsg.put ( key, val );
		return this;
	}

	/**
	 * Send the notification. If the notification is intended to be sent in the background,
	 * it's queued for send. Otherwise, it's sent in the current thread.
	 */
	public void send ()
	{
		withAddlData ( kQueuedAt, Clock.now () );

		final Sender mySender = new Sender ( fMsg );
		if ( fBackground )
		{
			if ( skBackgroundSender.isShutdown () )
			{
				throw new IllegalStateException ( "The ContinualNotifier background sender was shutdown." );
			}
			skBackgroundSender.submit ( mySender );
		}
		else
		{
			mySender.run ();
		}
	}

	private String fTopic;
	private String fStream = null;
	private final JSONObject fMsg = new JSONObject ();
	private String fUser;
	private String fPassword;
	private boolean fBackground = true;

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
		public Sender ( JSONObject msg )
		{
			fSenderMsg = JsonUtil.clone ( msg );
		}

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
					urlText = urlText + "/" + URLEncoder.encode ( fTopic, StandardCharsets.UTF_8 );
					if  ( fStream != null )
					{
						urlText = urlText + "/" + URLEncoder.encode ( fStream, StandardCharsets.UTF_8 );
					}
				}

				final URL url = new URL ( urlText );
				final byte[] payload = fSenderMsg
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

		private final JSONObject fSenderMsg;
	}
	
	private static final String skBaseUrlStr = "https://rcvr.continual.io/events";
	private static final ExpressionEvaluator skEval = new ExpressionEvaluator ( new EnvDataSource () );

	private static final Logger log = LoggerFactory.getLogger ( ContinualNotifier.class );

	private static final ExecutorService skBackgroundSender = Executors.newSingleThreadExecutor ();

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
		return val == null || val.isEmpty() ? null : val;
	}
}
