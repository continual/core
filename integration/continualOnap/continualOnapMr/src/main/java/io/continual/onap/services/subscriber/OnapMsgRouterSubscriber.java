package io.continual.onap.services.subscriber;

import java.io.IOException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import org.slf4j.Logger;

import io.continual.onap.services.mrCommon.Clock;
import io.continual.onap.services.mrCommon.CommonClientBuilder;
import io.continual.onap.services.mrCommon.HostSelector;
import io.continual.onap.services.mrCommon.HttpHelper;
import io.continual.onap.services.mrCommon.HttpHelper.Credentials;
import io.continual.onap.services.mrCommon.JsonResponseParser;
import io.continual.onap.services.mrCommon.SimpleJsonResponseParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A simple Cambria  subscriber. This class issues HTTP transactions that execute in the foreground
 * to push messages to the ONAP Message Router service.
 */
public class OnapMsgRouterSubscriber
{
	/**
	 * A builder for the subscriber.
	 */
	public static class Builder extends CommonClientBuilder
	{
		public static final int NO_RECV_LIMIT = -1;
		
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
		 * Specify the subscriber group that this subscriber belongs to.
		 * @param group the subscriber group name
		 * @return this builder
		 */
		public Builder inGroup ( String group )
		{
			fSubGroup = group;
			return this;
		}
		
		/**
		 * Specify the subscriber id for this subscriber. This must be unique across members of the group.
		 * @param id the unique subscriber ID in the group
		 * @return this builder
		 */
		public Builder withSubscriberId ( String id )
		{
			fSubId = id;
			return this;
		}

		/**
		 * Specify the amount of time to wait at the server for messages. This is essentially a long-poll
		 * mechanism supported by the ONAP Message Router system.
		 * @param ms the number of milliseconds to wait at the server for messages.
		 * @return this builder
		 */
		public Builder waitAtServerAtMost ( long ms )
		{
			fServerWaitMs = ms;
			return this;
		}

		/**
		 * Specify the maximum number of events to return on a fetch.
		 * @param eventCount the number of events to return at most
		 * @return this builder
		 */
		public Builder recvAtMostEvents ( int eventCount )
		{
			fMaxEventsPerFetch = eventCount;
			return this;
		}

		/**
		 * Specify the log to use. If never called, the default logger, named for this class, is used.
		 * @param log the slf4j logger to use for this library. Do not pass null.
		 * @return this builder
		 */
		public Builder logTo ( Logger log )
		{
			super.logTo ( log );
			return this;
		}


		/**
		 * Do not specify a maximum number of events to return on a fetch, allowing the
		 * server to determine the number to send.
		 * @return this builder
		 */
		public Builder noRecvLimit ()
		{
			return recvAtMostEvents ( NO_RECV_LIMIT );
		}

		/**
		 * Specify the amount of time to wait for a transaction to complete.
		 * @param ms the number of milliseconds to wait for transaction. Set to a negative value to use the default.
		 * @return this builder
		 */
		public Builder transactionTimeAtMost ( long ms )
		{
			super.transactionTimeAtMost ( ms );
			return this;
		}

		/**
		 * Specify the amount of time to wait on a socket connection, read, or write.
		 * @param ms the number of milliseconds to wait for a socket operation (connect/read/write)
		 * @return this builder
		 */
		@Override
		public Builder socketWaitAtMost ( long ms )
		{
			super.socketWaitAtMost ( ms );
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
		 * Provide a response parser.
		 * @param p the reponse parser
		 * @return this builder
		 */
		public Builder parseWith ( JsonResponseParser p )
		{
			fResponseParser = p;
			return this;
		}

		/**
		 * Build the subscriber given this specification.
		 * @return a new subscriber
		 */
		public OnapMsgRouterSubscriber build ()
		{
			return new OnapMsgRouterSubscriber ( this );
		}

		private String fSubGroup = null;
		private String fSubId = null;
		private long fServerWaitMs = 15000L;	// proactively make sure this caller does something reasonable
		private int fMaxEventsPerFetch = NO_RECV_LIMIT;
		private JsonResponseParser fResponseParser = new SimpleJsonResponseParser ();
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

	@Override
	public String toString ()
	{
		return fLabel;
	}

	public OnapMrFetchResponse fetch ( )
	{
		return fetch ( -1L, -1 );
	}

	public OnapMrFetchResponse fetch ( long waitAtServerMs, int maxEventsToFetch )
	{
		// if not specified explicitly in this call, use instance settings
		if ( waitAtServerMs < 0L )
		{
			waitAtServerMs = fServerWaitMs;
		}
		if ( maxEventsToFetch < 0 )
		{
			maxEventsToFetch = fMaxEventsPerFetch;
		}
		
		// read from MR, trying each host in order until we have a conclusion...

		final ArrayList<String> hostsLeft = new ArrayList<> ();
		fHosts.copyInto ( hostsLeft );

		final long noResponseTimeoutMs = fClock.nowMs () + fTrxWaitTimeoutMs;
		while ( fClock.nowMs () < noResponseTimeoutMs && !hostsLeft.isEmpty () )
		{
			final String host = hostsLeft.remove ( 0 );
			final String path = buildPath ( host, waitAtServerMs, maxEventsToFetch );

			final Request.Builder reqBuilder = new Request.Builder ()
				.url ( path )
				.get ()
			;
			HttpHelper.addAuth ( reqBuilder, fCreds, fClock );
			final Request req = reqBuilder.build ();

			fLog.info ( "GET {} ({})", path, fCreds.getUserDescription () );

			final long trxStartMs = fClock.nowMs ();
			try ( Response response = fHttpClient.newCall ( req ).execute () )
			{
				final long trxEndMs = fClock.nowMs ();
				final long trxDurationMs = trxEndMs - trxStartMs;

				final int statusCode = response.code ();
				final String statusText = response.message ();

				fLog.info ( "    Cambria reply {} {} ({} ms)", statusCode, statusText, trxDurationMs );

				if ( HttpHelper.isSuccess ( statusCode ) )
				{
					// process the response body into strings
					final OnapMrFetchResponse fetchResponse = new OnapMrFetchResponse ( statusCode, statusText );
					final ResponseBody body = response.body ();
					if ( body != null )
					{
						fResponseParser.parseResponseBody ( body, fetchResponse );
					}
					return fetchResponse;
				}
				else if ( HttpHelper.isClientFailure ( statusCode ) )
				{
					// just relay MR's reply
					return new OnapMrFetchResponse ( statusCode, statusText, new LinkedList<String> () );
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

				fLog.warn ( "    Cambria failure for host [{}]: {} ({} ms)", host, x.getMessage (), trxDurationMs );
				fHosts.demote ( host );
			}
			catch ( Throwable t )
			{
				fLog.warn ( "    Throwable {}", t.getMessage (), t );
				throw t;
			}
		}

		// if we're here, we've timed out on all Cambria hosts and we have to fail the transaction.
		return new OnapMrFetchResponse ( HttpHelper.k503_serviceUnavailable, "No Cambria  server could acknowledge the request.", new LinkedList<String> () );
	}

	/**
	 * Build a URL path for the Cambria service, provided protocol, port, and path as needed
	 * @param host the host to use
	 * @return a complete URL path
	 */
	private String buildPath ( String host, long waitAtServerMs, int maxEventsToFetch )
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
		sb
			.append ( "/events/" )
			.append ( HttpHelper.urlEncode ( fTopic ) )
			.append ( "/" )
			.append ( HttpHelper.urlEncode ( fSubGroup ) )
			.append ( "/" )
			.append ( HttpHelper.urlEncode ( fSubId ) )
		;

		boolean argsAdded = false;

		if ( waitAtServerMs > -1L )
		{
			sb
				.append ( "?timeout=" )
				.append ( waitAtServerMs )
			;
			argsAdded = true;
		}

		if ( maxEventsToFetch > -1L )
		{
			sb
				.append ( argsAdded ? "&" : "?" )
				.append ( "limit=" )
				.append ( maxEventsToFetch )
			;
			argsAdded = true;
		}

		return sb.toString ();
	}

	Clock getClock () { return fClock; }

	private final HostSelector fHosts;
	private final String fTopic;
	private final String fSubGroup;
	private final String fSubId;
	private final long fServerWaitMs;
	private final int fMaxEventsPerFetch;
	private final long fTrxWaitTimeoutMs;
	private final boolean fDefaultHttps;
	private final Credentials fCreds;
	private final String fLabel;
	private final Clock fClock;

	private final OkHttpClient fHttpClient;
	private final JsonResponseParser fResponseParser;

	private final Logger fLog;

	private OnapMsgRouterSubscriber ( Builder builder )
	{
		if ( builder.getHosts ()
			.isEmpty () ) throw new IllegalArgumentException ( "No hosts provided." );

		fHosts = HostSelector.builder ()
			.withHosts ( builder.getHosts () )
			.build ()
		;
		fDefaultHttps = builder.getDefaultHttps ();

		fCreds = builder.getCredentials ();
		if ( fCreds == null ) throw new IllegalArgumentException ( "No credentials instance provided." );

		fTopic = builder.getTopic();
		if ( fTopic == null || fTopic.length () < 1 ) throw new IllegalArgumentException ( "No topic provided." );

		fSubGroup = builder.fSubGroup;
		if ( fSubGroup == null || fSubGroup.length () < 1 ) throw new IllegalArgumentException ( "No subscription group provided." );

		fSubId = builder.fSubId == null ? UUID.randomUUID ().toString () : builder.fSubId;

		fServerWaitMs = builder.fServerWaitMs;
		fMaxEventsPerFetch = builder.fMaxEventsPerFetch;

		fTrxWaitTimeoutMs = builder.getTransactionWaitMs ();

		fLog = builder.getLog();
		if ( fLog == null ) throw new IllegalArgumentException ( "You must provide a logger." );

		fClock = builder.getClock ();

		// setup our HTTP client
		final long socketTimeoutMs = builder.getSocketWaitMs ();
		OkHttpClient.Builder okb = new OkHttpClient.Builder ()
			.connectTimeout ( socketTimeoutMs, TimeUnit.MILLISECONDS )
			.writeTimeout ( socketTimeoutMs, TimeUnit.MILLISECONDS )
			.readTimeout ( socketTimeoutMs, TimeUnit.MILLISECONDS )
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

		fResponseParser = builder.fResponseParser;
		if ( fResponseParser == null )
		{
			throw new IllegalArgumentException ( "A response parser is required." );
		}

		fLabel = fTopic +
			" on " +
			fHosts.toString () +
			" as " +
			fCreds.getUserDescription ()
		;
	}
}
