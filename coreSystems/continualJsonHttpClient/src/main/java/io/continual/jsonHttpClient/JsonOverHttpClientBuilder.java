package io.continual.jsonHttpClient;

import java.net.Proxy;

import org.json.JSONObject;

import io.continual.jsonHttpClient.impl.ok.OkHttp;

/**
 * A builder that creates a JsonOverHttpClient object using our default implementation.
 */
public class JsonOverHttpClientBuilder
{
	public static final String kCertValidation = "certValidation";

	public static final String kProxy = "proxy";
	public static final String kProxyHost = "host";
	public static final String kProxyPort = "port";
	public static final String kProxyType = "type";

	/**
	 * Construct a default builder
	 */
	public JsonOverHttpClientBuilder () {}

	/**
	 * Read a JSON config block and overwrite any builder settings using this config.
	 * @param config
	 * @return this builder
	 */
	public JsonOverHttpClientBuilder readJsonConfig ( JSONObject config )
	{
		enableCertValidation ( config.optBoolean ( kCertValidation, true ) );

		final JSONObject proxy = config.optJSONObject ( kProxy );
		if ( proxy != null )
		{
			final String host = proxy.optString ( kProxyHost );
			final int port = proxy.optInt ( kProxyPort, 8888 );
			if ( host != null && port > 0 )
			{
				Proxy.Type type = Proxy.Type.HTTP;

				final String typeStr = proxy.optString ( kProxyType, Proxy.Type.HTTP.toString () );
				if ( typeStr != null )
				{
					if ( typeStr.equalsIgnoreCase ( Proxy.Type.HTTP.toString ()  ) )
					{
						type = Proxy.Type.HTTP;
					}
					else if ( typeStr.equalsIgnoreCase ( Proxy.Type.SOCKS.toString ()  ) )
					{
						type = Proxy.Type.SOCKS;
					}
					else
					{
						throw new IllegalArgumentException ( "Unexpected proxy type " + typeStr );
					}
				}
				usingProxy ( new Proxy ( type, new java.net.InetSocketAddress ( host, port ) ) );
			}
		}

		return this;
	}

	/**
	 * Register a proxy to be used with this client
	 * @param p
	 * @return this builder
	 */
	public JsonOverHttpClientBuilder usingProxy ( Proxy p ) { fProxy = p; return this; }

	/**
	 * Enable certificate validation
	 * @return this builder
	 */
	public JsonOverHttpClientBuilder enableCertValidation () { return enableCertValidation ( true ); }

	/**
	 * Disable certificate validation
	 * @return this builder
	 */
	public JsonOverHttpClientBuilder disableCertValidation () { return enableCertValidation ( false ); }

	/**
	 * Enable certificate validation based on the given value
	 * @param enable
	 * @return this builder
	 */
	public JsonOverHttpClientBuilder enableCertValidation ( boolean enable ) { fCertValidation = enable; return this; }

	/**
	 * Build the default client implementation. Override this in a specialization class to use something different.
	 * @return a client object
	 */
	public JsonOverHttpClient build ()
	{
		return new OkHttp ( fProxy, !fCertValidation );
	}

	private Proxy fProxy = null;
	private boolean fCertValidation = true;
}
