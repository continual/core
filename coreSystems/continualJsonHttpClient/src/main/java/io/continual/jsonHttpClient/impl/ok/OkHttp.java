package io.continual.jsonHttpClient.impl.ok;

import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.jsonHttpClient.JsonOverHttpClient;
import okhttp3.OkHttpClient;

public class OkHttp implements JsonOverHttpClient
{
	public OkHttp ()
	{
		this ( null, false );
	}

	public OkHttp ( Proxy proxy )
	{
		this ( proxy, false );
	}

	public OkHttp ( Proxy proxy, boolean ignoreCertValidation )
	{
		fProxy = proxy;
		fIgnoreCertValidation = ignoreCertValidation;
	}

	@Override
	public HttpRequest newRequest ()
	{
		return new OkRequest ( getHttpClient () );
	}

	// FIXME: set timeout values
	
	private final Proxy fProxy;
	private OkHttpClient fHttpClient;
	private boolean fIgnoreCertValidation;

	private OkHttpClient getHttpClient ( )
	{
		if ( fHttpClient == null )
		{
			try
			{
				OkHttpClient.Builder builder = new OkHttpClient.Builder ()
					.connectTimeout ( 60, TimeUnit.SECONDS )
					.writeTimeout ( 60, TimeUnit.SECONDS )
					.readTimeout ( 60, TimeUnit.SECONDS )
					.proxy ( fProxy )
				;
	
				if ( fIgnoreCertValidation )
				{
					final TrustManager[] trustAllCerts = new TrustManager[]
					{
					    new X509TrustManager()
					    {
					        @Override
					        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
	
					        @Override
					        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
	
					        @Override
					        public java.security.cert.X509Certificate[] getAcceptedIssuers()
					        {
					            return new java.security.cert.X509Certificate[]{};
					        }
					    }
					};
	
					final SSLContext sslContext = SSLContext.getInstance ( "SSL" );
					sslContext.init ( null, trustAllCerts, new java.security.SecureRandom () );
					
					builder.sslSocketFactory ( sslContext.getSocketFactory (), (X509TrustManager) trustAllCerts[0] );
					builder.hostnameVerifier ( ( hostname, session ) -> true );
				}
				
				fHttpClient = builder.build ();
			}
			catch ( NoSuchAlgorithmException | KeyManagementException x )
			{
				log.warn ( "Couldn't ignore cert validation: " + x.getMessage () );
			}
		}
		return fHttpClient;
	}

	private static final Logger log = LoggerFactory.getLogger ( OkHttp.class );
}
