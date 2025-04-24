package io.continual.onap.services.mrCommon;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.onap.services.mrCommon.HttpHelper.Credentials;

public class CommonClientBuilder
{
	/**
	 * Add a host to the set the publisher can use. If you do not provide a protocol,
	 * "http://" is assumed. You may specify "https://" or "http://". If you do not 
	 * provide a port (e.g. "host:3904"), then 3904 is assumed for http, and 3905
	 * for https.  Thus "localhost" is treated as "http://localhost:3904".
	 * 
	 * @param host the host to add to the host set
	 * @return this builder
	 */
	public CommonClientBuilder withHost ( String host )
	{
		if ( host == null || host.isEmpty () )
		{
			throw new IllegalArgumentException ( "Invalid host value." );
		}
		fHosts.add ( host );
		return this;
	}

	/**
	 * Add each host to the host list. See withHost ( String host ) for details. 
	 * @param hosts a collection of hosts to add to the host set
	 * @return this builder
	 */
	public CommonClientBuilder withHosts ( Collection<String> hosts )
	{
		for ( String host : hosts )
		{
			withHost ( host );
		}
		return this;
	}
	
	/**
	 * Add each host to the host list. See withHost ( String host ) for details. 
	 * @param hosts a collection of hosts to add to the host set
	 * @return this builder
	 */
	public CommonClientBuilder withHosts ( String[] hosts )
	{
		for ( String host : hosts )
		{
			withHost ( host );
		}
		return this;
	}

	/**
	 * Clear any hosts the builder already knows about.
	 * @return this builder
	 */
	public CommonClientBuilder forgetHosts ()
	{
		fHosts.clear ();
		return this;
	}

	/**
	 * If no protocol is provided on a host string, default to http://
	 * @return this builder
	 */
	public CommonClientBuilder defaultHttp ()
	{
		return defaultHttps ( false );
	}

	/**
	 * If no protocol is provided on a host string, default to https://
	 * @return this builder
	 */
	public CommonClientBuilder defaultHttps ()
	{
		return defaultHttps ( true );
	}

	/**
	 * If no protocol is provided on a host string, default to https:// if true,
	 * http:// if false.
	 * @param https if true, use https. if false, use http
	 * @return this builder
	 */
	public CommonClientBuilder defaultHttps ( boolean https )
	{
		fDefaultHttps = https;
		return this;
	}

	/**
	 * Specify a proxy to use for the HTTP connection to Message Router.
	 * @param proxy a proxy string, which can optionally end in :port, e.g. proxy.example.com:8888
	 * @return this builder
	 */
	public CommonClientBuilder usingProxy ( String proxy )
	{
		if ( proxy == null ) { return usingProxy ( null, 8888 ); }
		
		final int colon = proxy.indexOf ( ':' );
		if ( colon > -1 )
		{
			return usingProxy ( proxy.substring ( 0, colon ), Integer.parseInt ( proxy.substring ( colon+1 ) ) );
		}
		return usingProxy ( proxy, 8888 );
	}

	/**
	 * Specify a proxy to use for the HTTP connection to Message Router.
	 * @param host a proxy host name
	 * @param port a port number
	 * @return this builder
	 */
	public CommonClientBuilder usingProxy ( String host, int port )
	{
		fProxyHost = host;
		fProxyPort = port;
		return this;
	}

	/**
	 * Specify the topic to publish to.
	 * @param topic the topic on which to post messages
	 * @return this builder
	 */
	public CommonClientBuilder onTopic ( String topic )
	{
		fTopic = topic;
		return this;
	}

	/**
	 * Set HTTP basic auth credentials. If user is null, the auth info is removed from the builder.
	 * @param user the username for basic auth credentials
	 * @param pwd  the password for basic auth credentials
	 * @return this builder
	 */
	public CommonClientBuilder asUser ( String user, String pwd )
	{
		fCreds = user == null ? Credentials.anonymous () : Credentials.asUser ( user, pwd );
		return this;
	}

	/**
	 * Set an API key and secret. If the API key is null, the auth info is removed from the builder.
	 * @param apiKey the API key for the user
	 * @param apiSecret the API key's secret
	 * @return this builder
	 */
	public CommonClientBuilder withApiKey ( String apiKey, String apiSecret )
	{
		fCreds = apiKey == null ? Credentials.anonymous () : Credentials.withApiKey ( apiKey, apiSecret );
		return this;
	}

	/**
	 * Specify the amount of time to wait for a transaction to complete.
	 * @param ms the number of milliseconds to wait for transaction. Set to a negative value to use the default.
	 * @return this builder
	 */
	public CommonClientBuilder transactionTimeAtMost ( long ms )
	{
		if ( ms >= 0 )
		{
			fTrxTimeoutMs = ms;
		}
		else
		{
			fTrxTimeoutMs = kDefaultTrxWaitMs;
		}
		return this;
	}

	/**
	 * Specify the amount of time to wait on a socket connection, read, or write.
	 * @param ms the number of milliseconds to wait for a socket operation (connect/read/write). Set to a negative value to use the default.
	 * @return this builder
	 */
	public CommonClientBuilder socketWaitAtMost ( long ms )
	{
		if ( ms >= 0 )
		{
			fWaitTimeoutMs = ms;
		}
		else
		{
			fWaitTimeoutMs = kDefaultSocketWaitMs;
		}
		return this;
	}
	
	/**
	 * Specify the log to use. If never called, the default logger, named for this class, is used.
	 * @param log the slf4j logger to use for this library. Do not pass null.
	 * @return this builder
	 */
	public CommonClientBuilder logTo ( Logger log )
	{
		fLog = log;
		return this;
	}

	/**
	 * Specify a clock to use within this implementation.
	 * @param clock the clock to use for timing
	 * @return this builder
	 */
	public CommonClientBuilder withClock ( Clock clock )
	{
		fClock = clock;
		return this;
	}

	///////////////////////////////////////////////////////////////////////////

	public List<String> getHosts ()
	{
		return Collections.unmodifiableList ( fHosts );
	}

	public boolean getDefaultHttps () { return fDefaultHttps; }

	public boolean isProxied () { return fProxyHost != null; }
	public String getProxyHost () { return fProxyHost; }
	public int getProxyPort () { return fProxyPort; }

	public Proxy getProxy ()
	{
		if ( isProxied () )
		{
			return new Proxy ( Proxy.Type.HTTP, new InetSocketAddress ( getProxyHost(), getProxyPort() ) );
		}
		return null;
	}
	
	public String getTopic () { return fTopic; }

	public long getTransactionWaitMs () { return fTrxTimeoutMs; }

	public long getSocketWaitMs () { return fWaitTimeoutMs; }

	public Logger getLog () { return fLog; }

	public Credentials getCredentials () { return fCreds; }

	public Clock getClock () { return fClock; }

	private final LinkedList<String> fHosts = new LinkedList<> ();
	private boolean fDefaultHttps = false;
	private String fProxyHost = null;
	private int fProxyPort = 8888;
	private String fTopic = null;
	private Credentials fCreds = Credentials.anonymous ();
	private long fTrxTimeoutMs = kDefaultTrxWaitMs;
	private long fWaitTimeoutMs = kDefaultSocketWaitMs;
	private Logger fLog = defaultLog;
	private Clock fClock = new StdClock ();

	private static final Logger defaultLog = LoggerFactory.getLogger ( "io.continual.onap" );
	private static final long kDefaultTrxWaitMs = 60 * 1000L;
	private static final long kDefaultSocketWaitMs = 30 * 1000L;

	private static class StdClock implements Clock
	{
		@Override
		public long nowMs () { return System.currentTimeMillis (); }
	}
}
