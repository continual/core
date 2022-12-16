package io.continual.iam.impl.common;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CommonJsonApiKeyTest
{
	private JSONObject jsonObj;

	@Before
	public void setUp ()
	{
		jsonObj = new JSONObject ();
		jsonObj.put ( CommonJsonDb.kUserId , "user" );
		jsonObj.put ( CommonJsonDb.kSecret , "secret" );
		jsonObj.put ( CommonJsonDb.kCreateTsMs , System.currentTimeMillis() );
	}

	@Test
	public void testInitialize ()
	{
		Assert.assertNotNull ( CommonJsonApiKey.initialize ( "secret" , "user" ) );
	}

	@Test (expected = JSONException.class )
	public void testConstructor_Exception ()
	{
		final JSONObject invalidJsonObj = new JSONObject ();
		invalidJsonObj.put ( CommonJsonDb.kSecret , "secret" );
		new CommonJsonApiKey ( "id" , invalidJsonObj );
	}

	@Test
	public void testConstructor_Valid ()
	{
		final CommonJsonApiKey cjak = new CommonJsonApiKey ( "id" , jsonObj );
		Assert.assertNotNull ( cjak );
	}

	@Test
	public void testGetKey ()
	{
		final CommonJsonApiKey cjak = new CommonJsonApiKey ( "id" , jsonObj );
		Assert.assertEquals ( "id" , cjak.getKey () );
	}

	@Test
	public void testSecret ()
	{
		final CommonJsonApiKey cjak = new CommonJsonApiKey ( "id" , jsonObj );
		Assert.assertEquals ( "secret" , cjak.getSecret () );
	}

	@Test
	public void testUserId ()
	{
		final CommonJsonApiKey cjak = new CommonJsonApiKey ( "id" , jsonObj );
		Assert.assertEquals ( "user" , cjak.getUserId () );
	}

	@Test
	public void testGetCreationTimestamp ()
	{
		final CommonJsonApiKey cjak = new CommonJsonApiKey ( "id" , jsonObj );
		Assert.assertNotNull ( cjak.getCreationTimestamp() );
	}
}
