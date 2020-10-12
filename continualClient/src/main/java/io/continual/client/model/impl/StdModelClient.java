package io.continual.client.model.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import io.continual.client.common.CommonClient;
import io.continual.client.model.ModelClient;
import io.continual.client.model.ModelObjectLocator;
import io.continual.client.model.ModelReference;

public class StdModelClient extends CommonClient implements ModelClient
{
	public StdModelClient ( String url, String user, String passwd ) throws MalformedURLException
	{
		fClient = new OkHttpClient ();

		fUrl = url;
		fUser = user;
		fPwd = passwd;
	}

	public List<String> getModels ( String acctId ) throws IOException, ModelServiceException
	{
		final LinkedList<String> result = new LinkedList<String> ();

		// build the base request
		Request.Builder reqb = new Request.Builder ()
			.url ( makePath ( fUrl, "models", acctId ) )
		;

		// auth headers
		reqb = addUserAuth ( reqb, fUser, fPwd );
//			reqb = addApiAuth ( reqb );

		// execute
		final Response response = fClient.newCall ( reqb.build () ).execute ();
		if ( !response.isSuccessful () )
		{
			if ( 404 == response.code () )
			{
				return null;
			}
			throw new ModelServiceException ( response.message () );
		}

		final ResponseBody body = response.body ();
		try ( final InputStream is = body.byteStream () )
		{
			final JSONObject bodyObject = new JSONObject ( new JSONTokener ( is ) );
	
			final JSONArray models = bodyObject.optJSONArray ( "models" );
			if ( models == null )
			{
				throw new ModelServiceException ( "Malformed response." );
			}
	
			for ( int i=0; i<models.length (); i++ )
			{
				result.add ( models.getString ( i ) );
			}
		}
		return result;
	}

	public String getModelData ( String acctId, String modelName )
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void createModel ( String acctId, String modelName )
	{
		// TODO Auto-generated method stub
		
	}

	public void deleteModel ( String acctId, String modelName )
	{
		// TODO Auto-generated method stub
		
	}

	public ModelReference getObject ( ModelObjectLocator locator )
	{
		return new StdModelReference ( fClient, locator );
	}

	public void deleteObject ( ModelObjectLocator locator )
	{
		// TODO Auto-generated method stub
		
	}

	private final OkHttpClient fClient;
	private final String fUrl;
	private final String fUser;
	private final String fPwd;
}
