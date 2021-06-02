/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.http.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.context.CHttpRequest;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.util.time.Clock;

/**
 * Read a JSON content body from a request.
 */
public class JsonBodyReader
{
	public static long kDefaultTimeoutMs = 1000 * 10;	// 10 seconds
	public static long kMaxBytes = 1024 * 1024 * 32;	// 32MB

	/**
	 * Read the body of the request in the given context into a list of JSON objects.
	 * 
	 * @param context
	 * @return a list of 0 or more JSONObjects
	 * @throws IOException
	 * @throws JSONException
	 */
	public static List<JSONObject> readBodyForObjects ( CHttpRequestContext context ) throws IOException, JSONException
	{
		return readBodyForObjects ( context, null );
	}

	/**
	 * read the request in the given context for a list of objects, optionally from the value
	 * named by 'path' in a top-level object.
	 * @param context
	 * @param path
	 * @return a list of 0 or more JSON objects
	 * @throws JSONException
	 */
	public static List<JSONObject> readBodyForObjects ( CHttpRequestContext context, String path ) throws JSONException
	{
		try
		{
			byte[] bytes = readBytes ( context.request (), kDefaultTimeoutMs );
			return readBodyForObjects ( bytes, path );
		}
		catch ( IOException e )
		{
			return new LinkedList<>();
		}
	}

	/**
	 * read the bytes for objects. If the bytes contain a single JSON object and the path is
	 * not null, the objects are loaded from the value named by path rather than the top-level
	 * object.
	 *  
	 * @param bytes
	 * @param path
	 * @return a list of 0 or more JSON objects
	 * @throws IOException
	 * @throws JSONException
	 */
	public static List<JSONObject> readBodyForObjects ( final byte[] bytes, String path ) throws IOException, JSONException
	{
		final LinkedList<JSONObject> result = new LinkedList<>();

		// determine the first token in the stream to decide if we're reading a single
		// object or an array.
		boolean isSingleObject;
		{
			final ByteArrayInputStream s = new ByteArrayInputStream ( bytes );
			final JSONTokener t = new JSONTokener ( s );
			
			char c = t.next ();
			while ( Character.isWhitespace ( c ) ) c = t.next ();

			switch ( c )
			{
				case '{': isSingleObject = true; break;
				case '[': isSingleObject = false; break;
				default: throw new JSONException ( "Expected an object or an array of objects." );
			}
			s.close ();
		}

		if ( isSingleObject )
		{
			final String jsonStream = new String ( bytes, utf8 );
			final JSONObject o = new JSONObject ( jsonStream );

			if ( path != null )
			{
				final Object oo = o.opt ( path );
				if ( oo instanceof JSONObject )
				{
					result.add ( (JSONObject) oo );
				}
				else if ( oo instanceof JSONArray )
				{
					result.addAll ( readArrayForObjects ( (JSONArray) oo ) );
				}
				else
				{
					throw new JSONException ( "Couldn't read object at path [" + path + "]." );
				}
			}
			else
			{
				result.add ( o );
			}
		}
		else
		{
			final String jsonStream = new String ( bytes );
			final JSONArray a = new JSONArray ( jsonStream );
			result.addAll ( readArrayForObjects ( a ) );
		}

		return result;
	}

	/**
	 * Read the request in the given context for a single JSON object, waiting at most the
	 * default timeout in milliseconds.
	 * @param context
	 * @return a JSONObject
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONObject readBody ( CHttpRequestContext context ) throws IOException, JSONException
	{
		return readBody ( context, kDefaultTimeoutMs );
	}

	/**
	 * Read the request in the given context for a single JSON object, waiting at most the
	 * given number of milliseconds.
	 * @param context
	 * @param timeoutMs
	 * @return a JSONObject
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONObject readBody ( CHttpRequestContext context, long timeoutMs ) throws IOException, JSONException
	{
		return readBody ( context.request (), timeoutMs );
	}

	/**
	 * Read the given request for a single JSON object, waiting at most the default
	 * number of milliseconds.
	 * @param req
	 * @return a JSONObject
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONObject readBody ( CHttpRequest req ) throws IOException, JSONException
	{
		return readBody ( req, kDefaultTimeoutMs );
	}

	/**
	 * Read the given request for a single JSON object, waiting at most the
	 * given number of milliseconds.
	 * @param req
	 * @param timeoutMs
	 * @return a JSONObject
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONObject readBody ( CHttpRequest req, long timeoutMs ) throws IOException, JSONException
	{
		byte[] bytes = readBytes ( req, timeoutMs );
		final String jsonStream = new String ( bytes );
		return new JSONObject ( jsonStream );
	}

	/**
	 * Read bytes from the given request, waiting at most timeout milliseconds
	 * @param req
	 * @param timeoutMs
	 * @return an array of bytes
	 * @throws IOException
	 */
	public static byte[] readBytes ( CHttpRequest req, long timeoutMs ) throws IOException
	{
		final int clen = req.getContentLength ();
		log.trace ( "Incoming content-length is " + clen );
		if ( clen == -1 )
		{
			throw new IOException ( "The content length header must be provided." );
		}
		else if ( clen > kMaxBytes )
		{
			throw new IOException ( "Input too large." );
		}
	
		final ByteArrayOutputStream baos = new ByteArrayOutputStream ();
	
		final InputStream is = req.getBodyStream ();
		if ( is == null )
		{
			throw new IOException ( "No input stream on request." );
		}
	
		long totalBytesRead = 0;
		try
		{
			byte[] b = new byte [ 4096 ];
			int len ;
			long lastReadMs = Clock.now ();
			boolean complete = false;
	
			do
			{
				len = is.read ( b );
				if ( len > 0 )
				{
					lastReadMs = Clock.now ();
					totalBytesRead += len;
					if ( totalBytesRead > kMaxBytes )
					{
						throw new IOException ( "Input too large." );
					}
					baos.write ( b, 0, len );
					complete = ( totalBytesRead >= clen );
				}
				else if ( len == 0 )
				{
					if ( lastReadMs + timeoutMs < Clock.now () )
					{
						log.info ( "Read timed out. Total " + totalBytesRead + " bytes, content-length was " + clen );
						throw new IOException ( "Timed out waiting for input." );
					}
				}
			}
			while ( len != -1 && !complete );
		}
		finally
		{
			is.close ();
		}
	
		if ( totalBytesRead < clen )
		{
			throw new IOException ( "Expected " + clen + " bytes, received " + totalBytesRead );
		}
	
		// truncate input to content-length
		final byte[] bytes = new byte [ clen ];
		System.arraycopy ( baos.toByteArray(), 0, bytes, 0, clen );
		return bytes;
	}


	private static List<JSONObject> readArrayForObjects ( JSONArray a ) throws JSONException
	{
		final LinkedList<JSONObject> result = new LinkedList<>();
		final int len = a.length ();
		for ( int i=0; i<len; i++ )
		{
			final Object o = a.get ( i );
			if ( o instanceof JSONObject )
			{
				result.add ( (JSONObject) o );
			}
			else
			{
				throw new JSONException ( "Expected an object or an array of objects." );
			}
		}
		return result;
	}

	private static final Charset utf8 = Charset.forName ( "UTF-8" );
	private static final Logger log = LoggerFactory.getLogger ( JsonBodyReader.class );
}
