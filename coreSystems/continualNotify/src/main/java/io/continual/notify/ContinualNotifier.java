package io.continual.notify;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.data.StreamTools;
import io.continual.util.data.exprEval.EnvDataSource;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.standards.HttpStatusCodes;

/**
 * A minimal client for sending notifications to Continual's Rcvr system.
 */
public class ContinualNotifier
{
	/**
	 * Send a message to Continual using credentials and topic/stream data from the environment.
	 * @param msg
	 */
	public static void send ( String msg )
	{
		new ContinualNotifier()
			.withMessage ( msg )
			.send ()
		;
	}

	public ContinualNotifier ()
	{
		fUser = evalSetting ( "CONTINUAL_USER" );
		fPassword = evalSetting ( "CONTINUAL_PASSWORD" );
		fTopic = evalSetting ( "CONTINUAL_RCVR_TOPIC" );
		fStream = evalSetting ( "CONTINUAL_RCVR_STREAM" );
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

	public ContinualNotifier withMessage ( String msg )
	{
		if ( msg == null ) throw new IllegalArgumentException ( "Provide a text message." );
		return withMessage ( new JSONObject ().put ( "message", msg ) );
	}
	
	public ContinualNotifier withMessage ( JSONObject msg )
	{
		if ( msg == null ) throw new IllegalArgumentException ( "Provide a JSON object." );
		fMsg = JsonUtil.clone ( msg );
		return this;
	}

	public void send ()
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
			final byte[] payload = fMsg.toString ().getBytes ( StandardCharsets.UTF_8 );

			final HttpURLConnection conn = (HttpURLConnection) url.openConnection ();

			// posting JSON data
			conn.setRequestMethod ( "POST" );
			conn.setRequestProperty ( "Content-Type", "application/json" );

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
				debug ( "ok: " + responseCode + " " + response );
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

	private String fTopic = null;
	private String fStream = null;
	private JSONObject fMsg = new JSONObject ();
	private String fUser = null;
	private String fPassword = null;

	private static final String skBaseUrlStr = "https://rcvr.continual.io/events";
	private static final ExpressionEvaluator skEval = new ExpressionEvaluator ( new EnvDataSource () );

	private static final Logger log = LoggerFactory.getLogger ( ContinualNotifier.class );

	private static String formatLog ( String msg )
	{
		return "Continual.Notifier " + msg;
	}

	private static void debug ( String msg )
	{
		log.debug ( formatLog ( msg ) );
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
