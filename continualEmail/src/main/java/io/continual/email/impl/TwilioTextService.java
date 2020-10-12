
package io.continual.email.impl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.email.TextService;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.time.Clock;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TwilioTextService extends SimpleService implements TextService
{
	public TwilioTextService ( ServiceContainer sc, JSONObject config )
	{
		fTwilioUser = config.getString ( "twilioUser" );
		fTwilioPwd = config.getString ( "twilioKey" );
		fFrom = config.getString ( "twilioFromNumber" );

		// setup our HTTP client
		OkHttpClient.Builder okb = new OkHttpClient.Builder ()
			.connectTimeout ( 15, TimeUnit.SECONDS )
			.writeTimeout ( 15, TimeUnit.SECONDS )
			.readTimeout ( 30, TimeUnit.SECONDS )
		;
//		if ( builder.fProxyHost != null )
//		{
//			okb = okb.proxy ( new Proxy ( Proxy.Type.HTTP, new InetSocketAddress ( builder.fProxyHost, builder.fProxyPort ) ) );
//		}
		fHttpClient = okb
			.build ()
		;
	}

	@Override
	public void sendMessage ( SmsBuilder msg ) throws IOException
	{
		if ( !( msg instanceof TwilioSmsBuilder ) )
		{
			throw new IllegalArgumentException ( "builder is not from here" );
		}
		final TwilioSmsBuilder builder = (TwilioSmsBuilder) msg;

		
		final String msgUrl = new StringBuilder ()
			.append ( "https://api.twilio.com/2010-04-01/Accounts/" )
			.append ( fTwilioUser )
			.append ( "/Messages.json" )
			.toString ()
		;

		// build form encoded body
		final RequestBody formBody = new FormBody.Builder ()
			.add ( "To", builder.fTo )
			.add ( "From", fFrom )
			.add ( "Body", builder.fBody )
			.build ()
		;

		final Request req = new Request.Builder ()
			.url ( msgUrl )
			.addHeader ( "Authorization", Credentials.basic ( fTwilioUser, fTwilioPwd ) )
			.post ( formBody )
			.build ()
		;

		log.info ( "POST {} ({})", msgUrl, fTwilioUser );

		final long trxStartMs = Clock.now ();
		try ( Response response = fHttpClient.newCall ( req ).execute () )
		{
			final long trxEndMs = Clock.now ();
			final long trxDurationMs = trxEndMs - trxStartMs;

			final int statusCode = response.code ();
			final String statusText = response.message ();
			final String responseBody = response.body ().string ();

			log.info ( "    MR reply {} {} ({} ms): {}", statusCode, statusText, trxDurationMs, responseBody );

			if ( isSuccess ( statusCode ) )
			{
				log.info ( "Twilio text to {} ok.", builder.fTo );
			}
			else if ( isClientFailure ( statusCode ) || isServerFailure ( statusCode ) )
			{
				log.warn ( "Twilio error response: " + statusCode + ", " + statusText );
			}
		}
		catch ( IOException x )
		{
			final long trxEndMs = Clock.now ();
			final long trxDurationMs = trxEndMs - trxStartMs;

			log.warn ( "Twilio service error: {} ({} ms)" + x.getMessage (), trxDurationMs );
		}
	}

	@Override
	public SmsBuilder createMessage ()
	{
		return new TwilioSmsBuilder ();
	}

	private final OkHttpClient fHttpClient;
	private final String fTwilioUser;
	private final String fTwilioPwd;
	private final String fFrom;
	private static final Logger log = LoggerFactory.getLogger ( TwilioTextService.class );

	private static class TwilioSmsBuilder
		implements SmsBuilder
	{
		@Override
		public SmsBuilder to ( String phoneNumber )
		{
			fTo = phoneNumber;
			return this;
		}

		@Override
		public SmsBuilder withBody ( String body )
		{
			fBody = body;
			return this;
		}

		private String fTo;
		private String fBody;
	}

	public static final int k200_ok = 200;
	public static final int k202_accepted = 202;
	public static final int k300_multipleChoices = 300;
	public static final int k400_badRequest = 400;
	public static final int k500_internalServerError = 500;
	public static final int k503_serviceUnavailable = 503;

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

}
