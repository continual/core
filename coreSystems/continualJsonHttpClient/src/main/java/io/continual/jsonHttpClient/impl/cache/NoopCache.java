package io.continual.jsonHttpClient.impl.cache;

import io.continual.jsonHttpClient.JsonOverHttpClient.HttpResponse;
import io.continual.jsonHttpClient.ResponseCache;

/**
 * A placeholder cache that doesn't cache anything.
 */
public class NoopCache implements ResponseCache
{
	@Override
	public HttpResponse get ( String path )
	{
		return null;
	}

	@Override
	public void put ( String path, HttpResponse response )
	{
		// ignored
	}

	@Override
	public void remove ( String path )
	{
	}
}
