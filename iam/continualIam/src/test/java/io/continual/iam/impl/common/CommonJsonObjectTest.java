package io.continual.iam.impl.common;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.impl.jsondoc.JsonDocDb;

public class CommonJsonObjectTest
{
	@Test
	public void testSetValue ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			cji.setValue ( "key1" , "value1" );
			cji.setValue ( "key2" , "value2" );
			Assert.assertEquals ( "value1" , cji.getValue ( "key1" , "default" ) );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testGetValueStringDefault ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		Assert.assertEquals ( "default" , cji.getValue ( "key" , "default" ) );
	}

	@Test
	public void testGetValueBooleanDefault ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		Assert.assertTrue ( cji.getValue ( "key" , true ) );
	}

	@Test
	public void testGetValueBoolean ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			cji.setValue ( "key" , "true" );
			Assert.assertTrue ( cji.getValue ( "key" , false ) );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testRemoveValueNotExist ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			cji.removeValue ( "key" );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testRemoveValue ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			cji.setValue ( "key" , "value" );
			cji.removeValue ( "key" );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testGetUserData ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			Assert.assertNull ( cji.getUserData ( "key" ) );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testPutUserData ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			cji.putUserData ( "key" , "value" );
			Assert.assertEquals ( "value" , cji.getUserData ( "key" ) );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testRemoveUserData ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			cji.putUserData ( "key" , "value" );
			cji.removeUserData ( "key" );
			Assert.assertNull ( cji.getUserData ( "key" ) );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testGetAllUserData1 ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			Assert.assertEquals ( 0 , cji.getAllUserData ().size () );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testGetAllUserData2 ()
	{
		final CommonJsonIdentity cji =  new CommonJsonIdentity ( "userId" , new JSONObject () , new JsonDocDb () );
		try {
			cji.putUserData ( "user1" , "pass1" );
			Assert.assertEquals ( "pass1" , cji.getAllUserData ().get ( "user1" ) );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}
}
