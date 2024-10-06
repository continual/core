package io.continual.jsonHttpClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import io.continual.jsonHttpClient.JsonOverHttpClient.BodyFactory;
import io.continual.jsonHttpClient.JsonOverHttpClient.BodyFormatException;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpResponse;
import io.continual.util.standards.MimeTypes;

public class TestResponse implements HttpResponse
{
	public TestResponse ( int code, String msg )
	{
		this ( code, msg, null, null );
	}

	public TestResponse ( int code, String msg, JSONObject body )
	{
		this ( code, msg, MimeTypes.kAppJson, body.toString () );
	}

	public TestResponse ( int code, String msg, String contentType, String response )
	{
		fCode = code;
		fMsg = msg;
		fContentType = contentType == null ? MimeTypes.kAppGenericBinary : contentType;
		fBytes = response == null ? new byte[0] : response.getBytes ( StandardCharsets.UTF_8 );
	}

	@Override
	public int getCode () { return fCode; }

	@Override
	public String getMessage () { return fMsg; }

	@Override
	public void close () {}

	@Override
	public <T> T getBody ( BodyFactory<T> bf ) throws BodyFormatException
	{
		try ( final InputStream is = new ByteArrayInputStream ( fBytes ) )
		{
			return bf.getBody ( fBytes.length, fContentType, is );
		}
		catch ( IOException e )
		{
			throw new BodyFormatException ( e );
		}
	}

	private final int fCode;
	private final String fMsg;
	private final String fContentType;
	private final byte[] fBytes;
}
