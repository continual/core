package io.continual.client.common;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.okhttp.Request;

public abstract class CommonClient
{
	/**
	 * Make a URL path from string segments.
	 * @param urlPart
	 * @param args
	 * @return a URL path
	 */
	protected String makeUrl ( String urlPart, String... args )
	{
		final String pathPart = makePath ( args );
		
		final StringBuilder sb = new StringBuilder ();
		sb.append ( urlPart );
		if ( sb.length () > 0 && !sb.toString ().endsWith ( "/" ) )
		{
			sb.append ( "/" );
		}
		sb.append ( pathPart );
		return sb.toString ();
	}
	
	
	/**
	 * Make a URL path from string segments.
	 * @param args
	 * @return a URL path
	 */
	protected String makePath ( String... args )
	{
		final StringBuilder sb = new StringBuilder ();
		for ( String arg : args )
		{
			if ( sb.length () > 0 && !sb.toString ().endsWith ( "/" ) )
			{
				sb.append ( "/" );
			}
			sb.append ( urlEncode ( arg ) );
		}
		return sb.toString ();
	}

	protected Request.Builder addUserAuth ( Request.Builder reqb, String user, String pwd )
	{
		// possibly add authentication header
		if ( user != null && pwd != null )
		{
			log.debug ( "authenticating with HTTP Basic " + user );
			reqb = reqb.addHeader ( "Authorization", "Basic " + Base64.getEncoder ().encodeToString ( (user + ":" + pwd).getBytes () ) );
		}
		else if ( user != null || pwd != null )
		{
			log.warn ( "HTTP Basic Auth credentials are only partly provided. Ignored." );
		}
		return reqb;
	}

	protected Request.Builder addApiAuth ( Request.Builder reqb, String apiKey, String apiSecret, String extraText )
	{
		// possibly add API key header
		if ( apiKey != null && apiKey.length () > 0 && apiSecret != null && apiSecret.length () > 0 )
		{
			log.debug ( "authenticating with API key " + apiKey );

			final long nowMs = System.currentTimeMillis ();
			final Date date = new Date ( nowMs );
			final SimpleDateFormat sdf = new SimpleDateFormat ( kPreferredDateFormat );
			final String xDate = sdf.format ( date );

			reqb = reqb.addHeader ( "X-Date", xDate );

			final StringBuilder textToSign = new StringBuilder ();
			if ( extraText != null )
			{
				textToSign.append ( extraText ).append ( "." );
			}
			textToSign.append ( xDate );

			final String auth = apiKey + ":" + hmacSign ( textToSign.toString (), apiSecret );

			reqb = reqb.addHeader ( "X-Auth", auth );
		}
		else if ( ( apiKey != null && apiKey.length () > 0 ) || ( apiSecret != null && apiSecret.length () > 0 ) )
		{
			log.warn ( "HTTP API credentials are only partly provided. Ignored." );
		}
		return reqb;
	}

	private static final String kHmacSha1Algo = "HmacSHA1";

    private String hmacSign ( String message, String key )
	{
		try
		{
			final SecretKey secretKey = new SecretKeySpec ( key.getBytes (), kHmacSha1Algo );
			final Mac mac = Mac.getInstance ( kHmacSha1Algo );
			mac.init ( secretKey );
			final byte[] rawHmac = mac.doFinal ( message.getBytes () );
			return Base64.getEncoder ().encodeToString ( rawHmac );
		}
		catch ( InvalidKeyException e )
		{
			throw new RuntimeException ( e );
		}
		catch ( NoSuchAlgorithmException e )
		{
			throw new RuntimeException ( e );
		}
		catch ( IllegalStateException e )
		{
			throw new RuntimeException ( e );
		}
	}

    private static final Logger log = LoggerFactory.getLogger ( CommonClient.class );
	private static final String kPreferredDateFormat = "EEE, dd MMM yyyy HH:mm:ss zzz";

	public static String urlEncode ( String s )
	{
		if ( s == null ) return null;

		try
		{
			return URLEncoder.encode ( s, "UTF-8" );
		}
		catch ( UnsupportedEncodingException e )
		{
			throw new RuntimeException ( e );
		}
	}
}
