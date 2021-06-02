package io.continual.onap.services.mrCommon;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Request;

public class HttpHelper
{
	public static final int k200_ok = 200;
	public static final int k202_accepted = 202;
	public static final int k300_multipleChoices = 300;
	public static final int k400_badRequest = 400;
	public static final int k500_internalServerError = 500;
	public static final int k503_serviceUnavailable = 503;

	public static final OnapMrResponse skAccepted = new OnapMrResponse ( k202_accepted, "Accepted." );
	public static final OnapMrResponse skSvcUnavailable = new OnapMrResponse ( k503_serviceUnavailable, "No Message Router server could acknowledge the request." );

	public static boolean isSuccess ( int code )
	{
		return code >= k200_ok && code < k300_multipleChoices;
	}

	public static boolean isClientFailure ( int code )
	{
		return code >= k400_badRequest && code < k500_internalServerError;
	}

	public static boolean isServerFailure ( int code )
	{
		return code >= k500_internalServerError;
	}

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

	public static class Credentials
	{
		public static Credentials anonymous ()
		{
			return new Credentials ( false, null, null );
		}

		public static Credentials asUser ( String username, String password )
		{
			if ( username == null || password == null )
			{
				throw new IllegalArgumentException ( "Username and password must both be supplied." );
			}
			return new Credentials ( false, username, password );
		}

		public static Credentials withApiKey ( String apiKey, String apiSecret )
		{
			if ( apiKey == null || apiSecret == null )
			{
				throw new IllegalArgumentException ( "API key and secret must both be supplied." );
			}
			return new Credentials ( true, apiKey, apiSecret );
		}

		public String getUserDescription () { return isAnonymous() ? "anonymous" : fId; }
		public boolean isAnonymous () { return fId == null; }
		
		public final String fId;
		public final String fSecret;
		public final boolean fIsApiKey;

		private Credentials ( boolean isApiKey, String id, String secret )
		{
			fIsApiKey = isApiKey;
			fId = id;
			fSecret = secret;
		}
	}
	
	public static void addAuth ( Request.Builder reqBuilder, Credentials creds, Clock clock )
	{
		if ( creds != null && !creds.isAnonymous () )
		{
			if ( creds.fIsApiKey )
			{
				for ( Entry<String, String> header : makeApiKeyHeaders ( clock, creds.fId, creds.fSecret).entrySet () )
				{
					reqBuilder.addHeader ( header.getKey(), header.getValue() );
				}
			}
			else
			{
				reqBuilder.addHeader ( "Authorization",
					okhttp3.Credentials.basic ( creds.fId, creds.fSecret )
				);
			}
		}
	}

	public static Map<String,String> makeApiKeyHeaders ( Clock clock, String apiKey, String apiSecret )
	{
		final HashMap<String,String> result = new HashMap<String,String> ();

		final SimpleDateFormat sdf = new SimpleDateFormat ( kPreferredDateFormat );
		final String xDate = sdf.format ( new Date ( clock.nowMs () ) );

		result.put ( "X-CambriaDate", xDate );
		result.put ( "X-Date", xDate );

		final String signature = sign ( xDate, apiSecret );

		final String auth = apiKey + ":" + signature;
		result.put ( "X-CambriaAuth", auth );
		result.put ( "X-Auth", auth );

		return result;
	}

	public static String sign ( String message, String key )
	{
		try
		{
			final SecretKey secretKey = new SecretKeySpec ( key.getBytes (), kHmacSha1Algo );
			final Mac mac = Mac.getInstance ( kHmacSha1Algo );
			mac.init ( secretKey );
			final byte[] rawHmac = mac.doFinal ( message.getBytes () );
			return Base64.getEncoder().encodeToString ( rawHmac );
		}
		catch ( InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e )
		{
			throw new RuntimeException ( e );
		}
	}

	public static final String kPreferredDateFormat = "EEE, dd MMM yyyy HH:mm:ss zzz";
	private static final String kHmacSha1Algo = "HmacSHA1";
}
