package io.continual.jsonHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import io.continual.util.data.StreamTools;
import io.continual.util.data.json.CommentedJsonTokener;

/**
 * This is a simple abstraction of an HTTP client implementation that manipulates JSON resources. The
 * point is mainly to isolate our code from specific HTTP libraries, which helps us to embed into
 * systems with an existing preferred HTTP client like Apache or OkHttp.
 */
public interface JsonOverHttpClient
{
	/**
	 * An exception representing a failure to complete an operation.
	 */
	public class HttpServiceException extends Exception
	{
		public HttpServiceException ( String msg ) { super(msg); }
		public HttpServiceException ( Throwable t ) { super(t); }
		public HttpServiceException ( String msg, Throwable t ) { super(msg,t); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * An exception representing a body parsing problem.
	 */
	public class BodyFormatException extends Exception
	{
		public BodyFormatException ( Exception x ) { super(x); }
		public BodyFormatException ( String msg ) { super(msg); }
		public BodyFormatException ( String msg, Exception x ) { super(msg,x); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * An interface for creating a body instance given an HTTP payload.
	 * @param <T>
	 */
	interface BodyFactory<T>
	{
		T getBody ( long contentLength, String mimeType, InputStream byteStream ) throws BodyFormatException;
	}

	/**
	 * A class to create JSON objects from HTTP payloads.
	 */
	public static class JsonObjectBodyFactory implements BodyFactory<JSONObject>
	{
		@Override
		public JSONObject getBody ( long contentLength, String mimeType, InputStream byteStream )
		{
			return new JSONObject ( new CommentedJsonTokener ( byteStream ) );
		}
	}
	
	/**
	 * A class to create JSON arrays from HTTP payloads.
	 */
	public static class JsonArrayBodyFactory implements BodyFactory<JSONArray>
	{
		@Override
		public JSONArray getBody ( long contentLength, String mimeType, InputStream byteStream )
		{
			return new JSONArray ( new CommentedJsonTokener ( byteStream ) );
		}
	}
	
	/**
	 * The HTTP call response 
	 */
	interface HttpResponse extends AutoCloseable
	{
		/**
		 * Get the HTTP status code.
		 * @return the HTTP status code
		 */
		int getCode ();

		/**
		 * Get the HTTP status message (e.g. "OK" in "200 OK")
		 * @return the HTTP status message
		 */
		String getMessage ();
		
		/**
		 * At this level, the close() call does not throw.
		 */
		@Override
	    void close ();

		/**
		 * Get the HTTP body as a JSON document
		 * @return a JSON document from the server
		 * @throws BodyFormatException 
		 */
		default JSONObject getBody () throws BodyFormatException
		{
			return getBody ( new JsonObjectBodyFactory () );
		}

		/**
		 * Get the HTTP body as a JSON array
		 * @return a JSON array from the server
		 * @throws BodyFormatException 
		 */
		default JSONArray getArrayBody () throws BodyFormatException
		{
			return getBody ( new JsonArrayBodyFactory () );
		}

		/**
		 * For troubleshooting, we can just read the response as a string
		 * @return a string
		 * @throws BodyFormatException 
		 */
		default String getStringBody () throws BodyFormatException
		{
			return getBody ( new BodyFactory<String> ()
			{
				@Override
				public String getBody ( long contentLength, String mimeType, InputStream byteStream ) throws BodyFormatException
				{
					try
					{
						final byte[] bytes = StreamTools.readBytes ( byteStream );
						return new String ( bytes, StandardCharsets.UTF_8 );
					}
					catch ( IOException e )
					{
						throw new BodyFormatException ( e );
					}
				}
			} );
		}
		
		/**
		 * Get the HTTP body as an arbitrary type
		 * @param <T>
		 * @param bf
		 * @return a T
		 * @throws BodyFormatException 
		 */
		<T> T getBody ( BodyFactory<T> bf ) throws BodyFormatException;
		
		/**
		 * Return true if the call was a success
		 * @return true if the HTTP call replied with a success code
		 */
		default boolean isSuccess ()
		{
			final int code = getCode();
			return code >= 200 && code < 300;
		}

		/**
		 * Return true if the server replied with 401 status. Note that isClientError() will 
		 * also return true in this case.
		 * @return true if the server replied with 401 status.
		 */
		default boolean isAuthError ()
		{
			return getCode() == 401;
		}

		/**
		 * Return true if the server replied "not found"
		 * @return true if the server replied "not found"
		 */
		default boolean isNotFound ()
		{
			return getCode() == 404;
		}

		/**
		 * Return true if the call resulted in a client-side error
		 * @return true if there was a client request error
		 */
		default boolean isClientError ()
		{
			final int code = getCode();
			return code >= 400 && code < 500;
		}

		/**
		 * Return true if the call resulted in a server-side error
		 * @return true if there was a server error
		 */
		default boolean isServerError ()
		{
			return getCode () >= 500;
		}
	}

	/**
	 * An HTTP request
	 */
	public interface HttpRequest
	{
		/**
		 * Specify the URL for the request
		 * @param url
		 * @return this request
		 */
		HttpRequest onPath ( String url );

		/**
		 * Specify the user credentials
		 * @param creds
		 * @return this request
		 */
		HttpRequest asUser ( HttpUsernamePasswordCredentials creds );

		/**
		 * Add a header to the request
		 * @param key
		 * @param value
		 * @return this request
		 */
		HttpRequest withHeader ( String key, String value );

		/**
		 * Add a set of headers to the request
		 * @param headers
		 * @return this request
		 */
		HttpRequest withHeaders ( Map<String,String> headers );

		/**
		 * Set a query string on the request, overwriting any other query settings.
		 * If this value is provided, prior calls to addQueryParam are ignored.
		 * @param qs
		 * @return this request
		 */
		HttpRequest withExplicitQueryString ( String qs );

		/**
		 * Add a query key/value to the request, preserving any existing settings. If
		 * withExplicitQueryString() has been called prior to this call, its setting is ignored.
		 * @param key
		 * @param val
		 * @return this request
		 */
		HttpRequest addQueryParam ( String key, String val );

		/**
		 * Add a map of query key/values to the request. This is equivalent to calling
		 * addQueryParam for each entry.
		 * @param qsMap
		 * @return this request
		 */
		HttpRequest withQueryString ( Map<String,String> qsMap );

		/**
		 * Execute a GET and return the response
		 * @return a response which must be closed
		 */
		HttpResponse get () throws HttpServiceException;

		/**
		 * Execute a DELETE and return the response
		 * @return a response which must be closed
		 */
		HttpResponse delete () throws HttpServiceException;

		/**
		 * Execute a PUT and return the response
		 * @param body the JSON to post
		 * @return a response which must be closed
		 */
		HttpResponse put ( JSONObject body ) throws HttpServiceException;

		/**
		 * Execute a PATCH and return the response
		 * @param body the JSON to post
		 * @return a response which must be closed
		 */
		HttpResponse patch ( JSONObject body ) throws HttpServiceException;

		/**
		 * Execute a POST and return the response
		 * @param body the JSON to post
		 * @return a response which must be closed
		 */
		HttpResponse post ( JSONObject body ) throws HttpServiceException;

		/**
		 * Execute a POST and return the response
		 * @param body the JSON to post
		 * @return a response which must be closed
		 */
		HttpResponse post ( JSONArray body ) throws HttpServiceException;
	}

	/**
	 * Start a request on this client
	 * @return a request
	 */
	HttpRequest newRequest ();

	/**
	 * Close this client. Calling newRequest() after close() is an illegal state.
	 */
	default void close () {}
}
