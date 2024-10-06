package io.continual.jsonHttpClient.impl.cache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import io.continual.jsonHttpClient.HttpUsernamePasswordCredentials;
import io.continual.jsonHttpClient.JsonOverHttpClient;
import io.continual.jsonHttpClient.ResponseCache;
import io.continual.util.data.StreamTools;
import io.continual.util.standards.MimeTypes;

/**
 * An implementation of the JsonOverHttpClient interface that uses an internal
 * cache. Both the base client implementation and the cache implementation must be provided.
 */
public class CachingClient implements JsonOverHttpClient
{
	public static class Builder
	{
		public Builder overClient ( JsonOverHttpClient baseClient )
		{
			fBaseClient = baseClient;
			return this;
		}
		
		public Builder withCache ( ResponseCache cache )
		{
			fCache = cache;
			return this;
		}

		public CachingClient build ()
		{
			return new CachingClient ( this );
		}

		private JsonOverHttpClient fBaseClient = null;
		private ResponseCache fCache = new ConcurrentMapCache.Builder ().build ();
		private CacheControl fControl = CacheControl.READ_AND_WRITE;
	};

	public enum CacheControl
	{
		/**
		 * Do not use the cache
		 */
		NO_CACHE,

		/**
		 * Read the cache for a potential result, but do not write a fetched result back to the cache
		 */
		READ_ONLY,

		/**
		 * Do not read the cache for a result but write a fetched result back to the cache
		 */
		WRITE_ONLY,

		/**
		 * Read the cache for a potential result and write a fetched result back to the cache
		 */
		READ_AND_WRITE;

		/**
		 * Is cache read allowed by this value?
		 * @return true if cache read is allowed
		 */
		public boolean allowRead ()
		{
			return this == READ_ONLY || this == READ_AND_WRITE;
		}

		/**
		 * Is cache write allowed by this value?
		 * @return true if cache write is allowed
		 */
		public boolean allowWrite ()
		{
			return this == WRITE_ONLY || this == READ_AND_WRITE;
		}
	};

	/**
	 * This is a specific request implementation that wraps the base client's request. 
	 */
	public class CachingRequest implements HttpRequest
	{
		@Override
		public HttpRequest onPath ( String url )
		{
			fPendingRequest.onPath ( url );
			fPath = url;
			return this;
		}

		@Override
		public HttpRequest asUser ( HttpUsernamePasswordCredentials creds )
		{
			fPendingRequest.asUser ( creds );
			return this;
		}

		@Override
		public HttpRequest withHeader ( String key, String value )
		{
			fPendingRequest.withHeader ( key, value );
			return this;
		}

		@Override
		public HttpRequest withHeaders ( Map<String, String> headers )
		{
			fPendingRequest.withHeaders ( headers );
			return this;
		}

		@Override
		public HttpRequest withExplicitQueryString ( String qs )
		{
			fPendingRequest.withExplicitQueryString ( qs );
			return this;
		}

		@Override
		public HttpRequest addQueryParam ( String key, String val )
		{
			fPendingRequest.addQueryParam ( key, val );
			return this;
		}

		@Override
		public HttpRequest withQueryString ( Map<String, String> qsMap )
		{
			fPendingRequest.withQueryString ( qsMap );
			return this;
		}

		public CachingRequest withCache ( CacheControl cc )
		{
			fCacheControl = cc;
			return this;
		}

		@Override
		public HttpResponse get () throws HttpServiceException
		{
			// check the cache
			final HttpResponse cachedResponse = readCache ();
			if ( cachedResponse != null )
			{
				return cachedResponse;
			}

			// execute request
			final HttpResponse resp = fPendingRequest.get ();

			// write to the cache
			return writeCache ( resp );
		}

		@Override
		public HttpResponse delete () throws HttpServiceException
		{
			removeFromCache ();
			return fPendingRequest.delete ();
		}

		@Override
		public HttpResponse put ( JSONObject body ) throws HttpServiceException
		{
			final HttpResponse resp = fPendingRequest.put ( body );
			if ( resp.isSuccess () )
			{
				writeCache ( body );
			}
			else
			{
				removeFromCache ();
			}
			return resp;
		}

		@Override
		public HttpResponse patch ( JSONObject body ) throws HttpServiceException
		{
			removeFromCache ();
			return fPendingRequest.patch ( body );
		}

		@Override
		public HttpResponse post ( JSONObject body ) throws HttpServiceException
		{
			removeFromCache ();
			return fPendingRequest.post ( body );
		}

		@Override
		public HttpResponse post ( JSONArray body ) throws HttpServiceException
		{
			removeFromCache ();
			return fPendingRequest.post ( body );
		}

		private HttpRequest fPendingRequest = fClient.newRequest ();
		private String fPath = null;
		private CacheControl fCacheControl = fDefaultCacheControl;

		private HttpResponse readCache ()
		{
			if ( fPath != null && fCacheControl.allowRead () )
			{
				final HttpResponse cachedResponse = fCache.get ( fPath );
				if ( cachedResponse != null )
				{
					return cachedResponse;
				}
			}
			return null;
		}

		private HttpResponse writeCache ( HttpResponse resp )
		{
			HttpResponse result = resp;
			if ( fCacheControl.allowWrite () )
			{
				result = wrap ( resp );
				fCache.put ( fPath, result );
			}
			return result;
		}

		private void writeCache ( JSONObject body )
		{
			if ( fCacheControl.allowWrite () )
			{
				fCache.put ( fPath, wrap ( body ) );
			}
		}

		private void removeFromCache ( )
		{
			if ( fPath != null )
			{
				fCache.remove ( fPath );
			}
		}
	}

	@Override
	public void close ()
	{
		fCache.close ();
	}

	/**
	 * Create a new HTTP request.
	 * @return a caching request
	 */
	@Override
	public CachingRequest newRequest ()
	{
		return new CachingRequest ();
	}

	private final JsonOverHttpClient fClient;
	private final ResponseCache fCache;
	private final CacheControl fDefaultCacheControl;

	private CachingClient ( Builder b )
	{
		fClient = b.fBaseClient;
		fCache = b.fCache;
		fDefaultCacheControl = b.fControl;

		if ( fClient == null ) throw new IllegalArgumentException ( "Missing base client." );
		if ( fCache == null ) throw new IllegalArgumentException ( "Missing cache implementation." );
	}

	private class CachedResponse implements HttpResponse
	{
		public CachedResponse ( int code, String msg, long contentLength, String mimeType, byte[] bytes )
		{
			fCode = code;
			fMsg = msg;
			fLength = contentLength;
			fMimeType = mimeType;
			fBytes = bytes;
			fEx = null;
		}

		public CachedResponse ( int code, String msg, BodyFormatException x )
		{
			fCode = code;
			fMsg = msg;
			fLength = -1L;
			fMimeType = null;
			fBytes = null;
			fEx = x;
		}

		@Override
		public void close () {}

		@Override
		public int getCode () { return fCode; }

		@Override
		public String getMessage () { return fMsg; }

		@Override
		public <T> T getBody ( BodyFactory<T> bf ) throws BodyFormatException
		{
			// if we stored an exception, throw that
			if ( fEx != null ) throw fEx;

			// otherwise transmit our cached body data
			try ( InputStream is = new ByteArrayInputStream ( fBytes ) )
			{
				return bf.getBody ( fLength, fMimeType, is );
			}
			catch ( IOException x )
			{
				throw new BodyFormatException ( x );
			}
		}

		private final int fCode;
		private final String fMsg;
		private final long fLength;
		private final String fMimeType;
		private final byte[] fBytes;
		private final BodyFormatException fEx;
	}

	private HttpResponse wrap ( HttpResponse r )
	{
		// pull the response data for repeated/later use
		try
		{
			return r.getBody ( new BodyFactory<CachedResponse> ()
			{
				@Override
				public CachedResponse getBody ( long contentLength, String mimeType, InputStream byteStream ) throws BodyFormatException
				{
					try
					{
						return new CachedResponse ( r.getCode (), r.getMessage (), contentLength, mimeType, StreamTools.readBytes ( byteStream ) );
					}
					catch ( IOException e )
					{
						throw new BodyFormatException ( e );
					}
				}
			} );
		}
		catch ( BodyFormatException e )
		{
			return new CachedResponse ( r.getCode (), r.getMessage (), e );
		}
	}

	private HttpResponse wrap ( JSONObject data )
	{
		final byte[] bytes = data.toString ().getBytes ( StandardCharsets.UTF_8 );
		return new CachedResponse ( 200, "OK", bytes.length, MimeTypes.kAppJson, bytes );
	}
}
