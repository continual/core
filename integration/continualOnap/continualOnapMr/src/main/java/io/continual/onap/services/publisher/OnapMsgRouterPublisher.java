package io.continual.onap.services.publisher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;

import io.continual.onap.services.mrCommon.Clock;
import io.continual.onap.services.mrCommon.CommonClientBuilder;
import io.continual.onap.services.mrCommon.HostSelector;
import io.continual.onap.services.mrCommon.HttpHelper;
import io.continual.onap.services.mrCommon.HttpHelper.Credentials;
import io.continual.onap.services.mrCommon.OnapMrResponse;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A simple message router publisher. This class issues HTTP transactions that execute in the foreground
 * to push messages to the ONAP Message Router service. 
 * 
 * This class is intended to be used concurrently. 
 */
public class OnapMsgRouterPublisher
{
	/**
	 * A builder for the publisher.
	 */
	public static class Builder extends CommonClientBuilder
	{
		public Builder () {}

		/**
		 * Add a host to the set the publisher can use. If you do not provide a protocol,
		 * "http://" is assumed. You may specify "https://" or "http://". If you do not 
		 * provide a port (e.g. "host:3904"), then 3904 is assumed for http, and 3905
		 * for https.  Thus "localhost" is treated as "http://localhost:3904".
		 * 
		 * @param host the host to add to the host set
		 * @return this builder
		 */
		@Override
		public Builder withHost ( String host )
		{
			super.withHost ( host );
			return this;
		}

		/**
		 * Add each host to the host list. See withHost ( String host ) for details. 
		 * @param hosts a collection of hosts to add to the host set
		 * @return this builder
		 */
		@Override
		public Builder withHosts ( Collection<String> hosts )
		{
			super.withHosts ( hosts );
			return this;
		}
		
		/**
		 * Add each host to the host list. See withHost ( String host ) for details. 
		 * @param hosts a collection of hosts to add to the host set
		 * @return this builder
		 */
		@Override
		public Builder withHosts ( String[] hosts )
		{
			super.withHosts ( hosts );
			return this;
		}

		/**
		 * Clear any hosts the builder already knows about.
		 * @return this builder
		 */
		@Override
		public Builder forgetHosts ()
		{
			super.forgetHosts ();
			return this;
		}

		/**
		 * Specify the topic to publish to.
		 * @param topic the topic on which to post messages
		 * @return this builder
		 */
		@Override
		public Builder onTopic ( String topic )
		{
			super.onTopic ( topic );
			return this;
		}

		/**
		 * Specify the amount of time to wait on a socket connection, read, or write.
		 * @param ms the number of milliseconds to wait for a socket operation (connect/read/write)
		 * @return this builder
		 */
		@Override
		public Builder waitingAtMost ( long ms )
		{
			super.socketWaitAtMost ( ms );
			return this;
		}

		/**
		 * Specify the log to use. If never called, the default logger, named for this class, is used.
		 * @param log the slf4j logger to use for this library. Do not pass null.
		 * @return this builder
		 */
		@Override
		public Builder logTo ( Logger log )
		{
			super.logTo ( log );
			return this;
		}

		/**
		 * Set HTTP basic auth credentials. If user is null, the auth info is removed from the builder.
		 * @param user the username for basic auth credentials
		 * @param pwd  the password for basic auth credentials
		 * @return this builder
		 */
		@Override
		public Builder asUser ( String user, String pwd )
		{
			super.asUser ( user, pwd );
			return this;
		}

		/**
		 * Set an API key and secret. If the API key is null, the auth info is removed from the builder.
		 * @param apiKey the API key for the user
		 * @param apiSecret the API key's secret
		 * @return this builder
		 */
		@Override
		public Builder withApiKey ( String apiKey, String apiSecret )
		{
			super.withApiKey ( apiKey, apiSecret );
			return this;
		}

		/**
		 * If no protocol is provided on a host string, default to http://
		 * @return this builder
		 */
		@Override
		public Builder defaultHttp ()
		{
			return defaultHttps ( false );
		}

		/**
		 * If no protocol is provided on a host string, default to https://
		 * @return this builder
		 */
		@Override
		public Builder defaultHttps ()
		{
			return defaultHttps ( true );
		}

		/**
		 * If no protocol is provided on a host string, default to https:// if true,
		 * http:// if false.
		 * @param https if true, use https. if false, use http
		 * @return this builder
		 */
		@Override
		public Builder defaultHttps ( boolean https )
		{
			super.defaultHttps ( https );
			return this;
		}

		/**
		 * Specify a clock to use within this implementation.
		 * @param clock the clock to use for timing
		 * @return this builder
		 */
		@Override
		public Builder withClock ( Clock clock )
		{
			super.withClock ( clock );
			return this;
		}

		/**
		 * Specify a proxy to use for the HTTP connection to Message Router.
		 * @param proxy a proxy string, which can optionally end in :port, e.g. proxy.example.com:8888
		 * @return this builder
		 */
		@Override
		public Builder usingProxy ( String proxy )
		{
			super.usingProxy ( proxy );
			return this;
		}

		/**
		 * Specify a proxy to use for the HTTP connection to Message Router.
		 * @param host a proxy host name
		 * @param port a port number
		 * @return this builder
		 */
		@Override
		public Builder usingProxy ( String host, int port )
		{
			super.usingProxy ( host, port );
			return this;
		}

		/**
		 * Build the publisher given this specification.
		 * @return a new publisher
		 */
		public OnapMsgRouterPublisher build ()
		{
			return new OnapMsgRouterPublisher ( this );
		}
	}

	/**
	 * Get a local test publisher builder to optionally customize further. By default, the publisher
	 * will run against http://localhost:3904, publishing to TEST-TOPIC.
	 * 
	 * @return a builder
	 */
	public static Builder localTest ()
	{
		return new Builder ()
			.withHost ( "localhost" )
			.onTopic ( "TEST-TOPIC" )
			.waitingAtMost ( 30000L )
		;
	}

	/**
	 * Get a new builder
	 * 
	 * @return a builder
	 */
	public static Builder builder ()
	{
		return new Builder ();
	}

	/**
	 * A message includes an event stream name and a payload
	 */
	public static class Message
	{
		public Message ( String eventStreamName, String payload )
		{
			fStreamName = eventStreamName;
			fData = payload;
		}

		public final String fStreamName;
		public final String fData;

		public byte[] getBytesForSend ()
		{
			return fData.toString ().getBytes ( kUtf8 );
		}
	}

	@Override
	public String toString ()
	{
		return fLabel;
	}

	/**
	 * Send a single message to the MR cluster.
	 * @param msg the message to post
	 * @return the HTTP status code from MR
	 */
	public OnapMrResponse send ( Message msg )
	{
		return send ( Collections.singletonList ( msg ) );
	}

	/**
	 * Send a set of messages to the MR cluster in an all or nothing attempt. Each host in
	 * the host list will be attempted at most once.
	 * 
	 * @param msgList a list of messages
	 * @return the HTTP status code from MR
	 */
	public OnapMrResponse send ( List<Message> msgList )
	{
		// if we have nothing to send, reply ok
		if ( msgList.size () < 1 ) return HttpHelper.skAccepted;

		// generate the transaction payload for content-type "application/cambria-zip"
		ByteArrayOutputStream baos;
		try
		{
			baos = new ByteArrayOutputStream ();
			final OutputStream wrapperOs = new GZIPOutputStream ( baos );
			for ( Message m : msgList )
			{
				final byte[] streamBytes = m.fStreamName.getBytes ( kUtf8 );
				final byte[] payloadBytes = m.getBytesForSend ();

				wrapperOs.write ( ( "" + streamBytes.length ).getBytes ( kUtf8 ) );
				wrapperOs.write ( '.' );
				wrapperOs.write ( ( "" + payloadBytes.length ).getBytes ( kUtf8 ) );
				wrapperOs.write ( '.' );
				wrapperOs.write ( streamBytes );
				wrapperOs.write ( payloadBytes );
				wrapperOs.write ( '\n' );
			}
			wrapperOs.close ();
			baos.close ();
		}
		catch ( IOException e )
		{
			// an I/O exception while building the request body isn't likely
			fLog.error ( "Error while building payload for MR publish. Returning 400 Bad Request. " + e.getMessage (), e );
			return new OnapMrResponse ( HttpHelper.k400_badRequest, "Unable to build payload." );
		}
		final byte[] msgBody = baos.toByteArray ();

		// send the data to MR, trying each host in order until we have a conclusion...

		final ArrayList<String> hostsLeft = new ArrayList<> ();
		fHosts.copyInto ( hostsLeft );

		final long noResponseTimeoutMs = fClock.nowMs () + fWaitTimeoutMs;
		while ( fClock.nowMs () < noResponseTimeoutMs && hostsLeft.size () > 0 )
		{
			final String host = hostsLeft.remove ( 0 );
			final String path = buildPath ( host );

			final RequestBody body = RequestBody.create ( msgBody, kCambriaZip );
			final Request.Builder reqBuilder = new Request.Builder ()
				.url ( path )
				.post ( body )
			;
			HttpHelper.addAuth ( reqBuilder, fCreds, fClock );
			final Request req = reqBuilder.build ();

			fLog.info ( "POST {} ({})", path, fCreds.getUserDescription () );

			final long trxStartMs = fClock.nowMs ();
			try ( Response response = fHttpClient.newCall ( req ).execute () )
			{
				final long trxEndMs = fClock.nowMs ();
				final long trxDurationMs = trxEndMs - trxStartMs;

				final int statusCode = response.code ();
				final String statusText = response.message ();
				final String responseBody = response.body ().string ();

				fLog.info ( "    MR reply {} {} ({} ms): {}", statusCode, statusText, trxDurationMs, formatJsonTextForLog ( responseBody ) );

				if ( HttpHelper.isSuccess ( statusCode ) || HttpHelper.isClientFailure ( statusCode ) )
				{
					// just relay MR's reply
					return new OnapMrResponse ( statusCode, statusText );
				}
				else if ( HttpHelper.isServerFailure ( statusCode ) )
				{
					// that host has a problem, move on
					fHosts.demote ( host );
				}
			}
			catch ( IOException x )
			{
				final long trxEndMs = fClock.nowMs ();
				final long trxDurationMs = trxEndMs - trxStartMs;

				fLog.warn ( "    MR failure for host [{}]: {} ({} ms)", host, x.getMessage (), trxDurationMs );
				fHosts.demote ( host );
			}
		}

		// if we're here, we've timed out on all MR hosts and we have to fail the transaction.
		return HttpHelper.skSvcUnavailable;
	}

	/**
	 * Build a URL path for Message Router, provided protocol, port, and path as needed
	 * @param host
	 * @return a complete URL path
	 */
	private String buildPath ( String host )
	{
		final StringBuilder sb = new StringBuilder ();

		// add a protocol if one is not provided
		if ( !host.contains ( "://" ) )
		{
			sb.append ( fDefaultHttps ? "https://" : "http://" );
		}

		// add the host
		sb.append ( host );

		// add a port if necessary
		if ( !host.contains ( ":" ) )
		{
			sb.append ( host.startsWith ( "https://" ) ? ":3905" : ":3904" );
		}

		// finally the path parts
		sb.append ( "/events/" );
		sb.append ( HttpHelper.urlEncode ( fTopic ) );

		return sb.toString ();
	}

	Clock getClock () { return fClock; }

	// all members should be immutable or thread-safe, because this class is expected to be
	// used by multiple threads without explicit locking.

	private final HostSelector fHosts;
	private final String fTopic;
	private final long fWaitTimeoutMs;
	private final Credentials fCreds;
	private final boolean fDefaultHttps;
	private final String fLabel;
	private final Clock fClock;

	private final OkHttpClient fHttpClient;

	private final Logger fLog;

	private static final MediaType kCambriaZip = MediaType.get ( "application/cambria-zip" );
	private static final Charset kUtf8 = Charset.forName ( "UTF-8" );

	private static String formatJsonTextForLog ( String text )
	{
		// we don't want to bother with the time to parse this as legit JSON... 
		return text.replaceAll ( "\\n", " " );
	}

	private OnapMsgRouterPublisher ( Builder builder )
	{
		if ( builder.getHosts().size () < 1 ) throw new IllegalArgumentException ( "No hosts provided." );
		if ( builder.getTopic() == null || builder.getTopic().length () < 1 ) throw new IllegalArgumentException ( "No topic provided." );

		fHosts = HostSelector.builder ()
			.withHosts ( builder.getHosts () )
			.build ()
		;

		fTopic = builder.getTopic();

		fWaitTimeoutMs = builder.getSocketWaitMs ();
		fDefaultHttps = builder.getDefaultHttps ();

		fCreds = builder.getCredentials ();
		
		if ( builder.getLog () == null ) throw new IllegalArgumentException ( "You must provide a logger." );
		fLog = builder.getLog ();

		fClock = builder.getClock();

		// setup our HTTP client
		OkHttpClient.Builder okb = new OkHttpClient.Builder ()
			.connectTimeout ( 15, TimeUnit.SECONDS )
			.writeTimeout ( 15, TimeUnit.SECONDS )
			.readTimeout ( 30, TimeUnit.SECONDS )
		;

		// setup proxy
		final Proxy proxy = builder.getProxy ();
		if ( proxy != null )
		{
			okb = okb.proxy ( proxy );
		}

		fHttpClient = okb
			.build ()
		;

		fLabel = new StringBuilder ()
			.append ( fTopic )
			.append ( " on " )
			.append ( fHosts.toString () )
			.append ( " as " )
			.append ( fCreds.isAnonymous () ? "anonymous" : fCreds.getUserDescription () )
			.toString ()
		;
	}

	public static final String kPreferredDateFormat = "EEE, dd MMM yyyy HH:mm:ss zzz";
}
