package io.continual.client.events.impl;

import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONArray;
import org.json.JSONObject;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import io.continual.client.common.CommonClient;
import io.continual.client.events.EventClient;


public class StdEventClient extends CommonClient implements EventClient
{
	public StdEventClient ( String user, String pwd, String apiKey, String apiSecret ) throws MalformedURLException
	{
		fClient = new OkHttpClient ();

		fUser = user;
		fPwd = pwd;
		fApiKey = apiKey;
		fApiSecret = apiSecret;
	}

	/**
	 * Send data to the Nebby event receiver
	 * @param data
	 * @throws EventServiceException
	 * @throws IOException
	 */
	public void send ( JSONObject data ) throws EventServiceException, IOException
	{
		runPost ( "events", data );
	}

	/**
	 * Send data to the Nebby event receiver
	 * @param data
	 * @param onTopic
	 * @throws EventServiceException
	 * @throws IOException
	 */
	public void send ( JSONObject data, String onTopic ) throws EventServiceException, IOException
	{
		runPost ( makePath ( "events", onTopic ), data );
	}

	/**
	 * Send data to the Nebby event receiver
	 * @param data
	 * @param onTopic
	 * @param onPartition
	 * @throws EventServiceException
	 * @throws IOException
	 */
	public void send ( JSONObject data, String onTopic, String onPartition ) throws EventServiceException, IOException
	{
		runPost ( makePath ( "events", onTopic, onPartition ), data );
	}

	/**
	 * Send data to the Nebby event receiver
	 * @param data
	 * @throws EventServiceException
	 * @throws IOException
	 */
	public void send ( JSONArray data ) throws EventServiceException, IOException
	{
		runPost ( makePath ( "events" ), data );
	}

	/**
	 * Send data to the Nebby event receiver
	 * @param data
	 * @param onTopic
	 * @throws EventServiceException
	 * @throws IOException
	 */
	public void send ( JSONArray data, String onTopic ) throws EventServiceException, IOException
	{
		runPost ( makePath ( "events", onTopic ), data );
	}

	/**
	 * Send data to the event receiver
	 * @param data
	 * @param onTopic
	 * @param onPartition
	 * @throws EventServiceException
	 * @throws IOException
	 */
	public void send ( JSONArray data, String onTopic, String onPartition ) throws EventServiceException, IOException
	{
		runPost ( makePath ( "events", onTopic, onPartition ), data );
	}

	private final OkHttpClient fClient;
	private final String fUser;
	private final String fPwd;
	private final String fApiKey;
	private final String fApiSecret;
	
	private void runPost ( String path, JSONObject data ) throws IOException, EventServiceException
	{
		runPost ( path, data.toString () );
	}

	private void runPost ( String path, JSONArray data ) throws IOException, EventServiceException
	{
		runPost ( path, data.toString () );
	}

	private void runPost ( String path, String data ) throws IOException, EventServiceException
	{
		final RequestBody body = RequestBody.create ( MediaType.parse ( "application/json; charset=utf-8" ), data );

		// build the base request
		Request.Builder reqb = new Request.Builder ()
			.url ( makeUrl ( "https://rcvr.continual.io/" + path ) )
			.post ( body )
		;

		// auth headers
		reqb = addUserAuth ( reqb, fUser, fPwd );
		reqb = addApiAuth ( reqb, fApiKey, fApiSecret, null );

		// execute
		final Response response = fClient.newCall ( reqb.build () ).execute ();
		if ( !response.isSuccessful () )
		{
			throw new EventServiceException ( response.message () );
		}
	}
}
