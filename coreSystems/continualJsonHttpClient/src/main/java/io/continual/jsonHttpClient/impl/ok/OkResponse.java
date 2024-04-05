package io.continual.jsonHttpClient.impl.ok;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.jsonHttpClient.JsonOverHttpClient.BodyFactory;
import io.continual.jsonHttpClient.JsonOverHttpClient.BodyFormatException;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpResponse;
import io.continual.util.data.json.CommentedJsonTokener;
import okhttp3.MediaType;
import okhttp3.Response;

class OkResponse implements HttpResponse
{
	public OkResponse ( int status, Response resp )
	{
		fStatus = status;
		fResponse = resp;
	}

	@Override
	public void close ()
	{
		if ( fResponse != null )
		{
			fResponse.close ();
		}
	}

	@Override
	public int getCode () { return fStatus; }

	@Override
	public String getMessage () { return fResponse.message (); }

	@Override
	public JSONObject getBody () throws BodyFormatException
	{
		return getBody ( new BodyFactory<JSONObject> ()
		{
			@Override
			public JSONObject getBody ( long length, String contentType, InputStream byteStream )
			{
				if ( length == 0 ) return new JSONObject ();
				try
				{
					return new JSONObject ( new CommentedJsonTokener ( byteStream ) );
				}
				catch ( JSONException e )
				{
					return new JSONObject ()
						.put ( "error", "couldn't read body from server" )
						.put ( "message", e.getMessage () )
					;
				}
			}
		} );
	}

	@Override
	public <T> T getBody ( BodyFactory<T> bf ) throws BodyFormatException
	{
		if ( fResponse == null ) return null;
		try
		{
			final long length = fResponse.body ().contentLength ();
			final MediaType mimeType = fResponse.body ().contentType ();
			final String contentEncoding = fResponse.header ( "Content-Encoding" );

			InputStream stream = fResponse.body ().byteStream ();
			if ( contentEncoding != null && contentEncoding.equalsIgnoreCase ( "gzip" ) )
			{
				try
				{
					stream = new GZIPInputStream ( stream );
				}
				catch ( IOException e )
				{
					throw new BodyFormatException ( e );
				}
			}

			return bf.getBody ( length, mimeType.toString (), stream );
		}
		finally
		{
			fResponse.close ();
		}
	}

	@Override
	public boolean isSuccess ()
	{
		final int code = getCode();
		return code >= 200 && code < 300;
	}

	private final int fStatus;
	private final Response fResponse;
}
