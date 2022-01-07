package io.continual.jsonHttpClient.impl.ok;

import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import io.continual.jsonHttpClient.JsonOverHttpClient;
import okhttp3.OkHttpClient;

public class OkHttp implements JsonOverHttpClient
{
	public OkHttp ()
	{
		this ( null );
	}

	public OkHttp ( Proxy proxy )
	{
		fProxy = proxy;
	}

	@Override
	public HttpRequest newRequest ()
	{
		return new OkRequest ( getHttpClient () );
	}

	private final Proxy fProxy;
	private OkHttpClient fHttpClient;

	private OkHttpClient getHttpClient ( )
	{
		if ( fHttpClient == null )
		{
			fHttpClient = new OkHttpClient.Builder ()
				.connectTimeout ( 60, TimeUnit.SECONDS )
				.writeTimeout ( 60, TimeUnit.SECONDS )
				.readTimeout ( 60, TimeUnit.SECONDS )
				.proxy ( fProxy )
				.build ()
			;
		}
		return fHttpClient;
	}
}
