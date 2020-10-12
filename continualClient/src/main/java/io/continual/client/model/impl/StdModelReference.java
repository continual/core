package io.continual.client.model.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

import com.squareup.okhttp.OkHttpClient;

import io.continual.client.model.ModelClient.ModelServiceException;
import io.continual.client.model.ModelObjectLocator;
import io.continual.client.model.ModelReference;
import io.continual.client.model.ModelRelation;

class StdModelReference implements ModelReference
{
	public StdModelReference ( OkHttpClient client, ModelObjectLocator locator )
	{
		fClient = client;
		fLocator = locator;
	}

	@Override
	public String getData () throws ModelServiceException, IOException
	{
//		return fClient
//			.get ( makeObjectPath () )
//			.fData.getJSONObject ( "data" )
//			.toString ()
//		;
		return "FIXME";
	}

	@Override
	public ModelReference putData ( String jsonData ) throws ModelServiceException, IOException
	{
//		fClient.put ( makeObjectPath (), new JSONObject ( new JSONTokener ( jsonData ) ) );
		return this;
	}

	@Override
	public ModelReference patchData ( String jsonData )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<ModelRelation> getRelations ()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<ModelRelation> getInboundRelations ()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<ModelRelation> getInboundRelations ( String named )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<ModelRelation> getOutboundRelations ()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<ModelRelation> getOutboundRelations ( String named )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModelReference relateTo ( String name, ModelObjectLocator to )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModelReference relateFrom ( String name, ModelObjectLocator from )
	{
		// TODO Auto-generated method stub
		return null;
	}

	private final OkHttpClient fClient;
	private final ModelObjectLocator fLocator;

	private String makeObjectPath ( ) throws UnsupportedEncodingException
	{
		return
			"/models/" +
			URLEncoder.encode ( fLocator.getAcctId() , "UTF-8" ) +
			"/" +
			URLEncoder.encode ( fLocator.getModel() , "UTF-8" ) +
			"/objects/" +
			URLEncoder.encode ( fLocator.getOid() , "UTF-8" ) +
			"/-/data"
		;
	}
}
