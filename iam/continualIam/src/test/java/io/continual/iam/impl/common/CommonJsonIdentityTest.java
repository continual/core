package io.continual.iam.impl.common;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamIdentityExists;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.impl.jsondoc.JsonDocDb;

public class CommonJsonIdentityTest
{
	@Test
	public void testInitializeIdentity ()
	{
		Assert.assertNotNull ( CommonJsonIdentity.initializeIdentity () );
		Assert.assertTrue ( CommonJsonIdentity.initializeIdentity ().getBoolean ( CommonJsonDb.kEnabled ) );
	}

	@Test
	public void testConstructor ()
	{
		Assert.assertNotNull ( new CommonJsonIdentity ( "userId" , null , null ) );
	}

	@Test
	public void testGetId ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , null , null );
		Assert.assertEquals ( "userId" , cji.getId () );
	}

	@Test
	public void testSetApiKeyUsedForAuth ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , null , null );
		cji.setApiKeyUsedForAuth ( "apikey" );
		Assert.assertEquals ( "apikey" , cji.getApiKey () );
	}

	@Test
	public void testToString ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , null , null );
		Assert.assertEquals ( "userId" , cji.toString () );
		cji.setApiKeyUsedForAuth ( "apikey" );
		Assert.assertTrue ( cji.toString ().contains ( "apikey" ) );
	}

	@Test
	public void testAsJson ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "name" , "continual" );
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , jsonObj , null );
		Assert.assertEquals ( jsonObj.length () , cji.asJson ().length () );
	}

	@Test
	public void testSetPassword ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			cji.setPassword ( "password" );
			Assert.assertTrue ( cji.asJson ().has ( CommonJsonDb.kPasswordBlock ) );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testRequestPasswordReset ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			cji.enable ( true );
			cji.requestPasswordReset ( 999000 , "nonce" );
		} catch (IamSvcException | IamBadRequestException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = IamBadRequestException.class )
	public void testRequestPasswordReset_Exception () throws IamBadRequestException
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			cji.requestPasswordReset ( 100 , "nonce" );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testCreateApiKey ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.createUser ( "userId" );
			final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , jdd );
			Assert.assertNotNull ( cji.createApiKey () );
		} catch (IamIdentityExists | IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = IamSvcException.class )
	public void testCreateApiKey_Exception () throws IamSvcException
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		cji.createApiKey ();
	}

	@Test
	public void testLoadApiKeysForUser ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.createUser ( "userId" );
			final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , jdd );
			cji.createApiKey ();
			Assert.assertNotNull ( cji.loadApiKeysForUser () );
		} catch (IamIdentityExists | IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = IamSvcException.class )
	public void testLoadApiKeysForUser_Exception () throws IamSvcException
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		cji.loadApiKeysForUser ();
	}

	@Test
	public void testDeleteApiKey ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.createUser ( "userId" );
			final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , jdd );
			cji.deleteApiKey ( cji.createApiKey () );
		} catch (IamIdentityExists | IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testGetPasswordSalt ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			Assert.assertNull ( cji.getPasswordSalt () );
			cji.setPassword ( "password" );
			Assert.assertNotNull ( cji.getPasswordSalt () );			
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testGetPasswordHash ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			Assert.assertNull ( cji.getPasswordHash () );
			cji.setPassword ( "password" );
			Assert.assertNotNull ( cji.getPasswordHash () );			
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testGetGroupIds ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			cji.addGroup ( "group1" );
			Assert.assertNotNull ( cji.getGroupIds () );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testGetGroups ()
	{
		final JSONObject jsonGroup = new JSONObject ();
		jsonGroup.put ( "group1" , new JSONObject() );
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "groups" , jsonGroup );
		try {
			final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb ( jsonObj ) );
			cji.addGroup ( "group1" );
			cji.addGroup ( "group2" );
			Assert.assertNotNull ( cji.getGroups () );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testGetGroup ()
	{
		final JSONObject jsonGroup = new JSONObject ();
		jsonGroup.put ( "group1" , new JSONObject() );
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "groups" , jsonGroup );
		try {
			final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb ( jsonObj ) );
			cji.addGroup ( "group1" );
			Assert.assertNotNull ( cji.getGroup ( "group1" ) );
			Assert.assertNull ( cji.getGroup ( "group2" ) );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testAddApiKey ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		cji.addApiKey ( "apikey1" );
	}

	@Test
	public void testAddGroup ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		Assert.assertTrue ( cji.addGroup ( "group1" ) );	//	New Group added.
		Assert.assertFalse ( cji.addGroup ( "group1" ) );	//	Group already exist.
	}

	@Test
	public void testRemoveGroup ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		cji.addGroup ( "group1" );
		Assert.assertTrue ( cji.removeGroup ( "group1" ) );
	}	

	@Test
	public void testGetDataRecord ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		cji.addGroup ( "group1" );
		Assert.assertTrue ( cji.getDataRecord().length() > 0 );
	}

	@Test
	public void testReload ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		cji.addGroup ( "group1" );
		try {
			cji.reload ();
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}
	
	@Test
	public void testStore ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		cji.addGroup ( "group1" );
		try {
			cji.store ();
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testMain ()
	{
		CommonJsonIdentity.main ( new String[] { "user" , "password" } );
		CommonJsonIdentity.main ( new String[] { "password" } );
	}
}
