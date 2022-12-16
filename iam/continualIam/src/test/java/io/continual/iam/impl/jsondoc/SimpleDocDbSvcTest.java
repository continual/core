package io.continual.iam.impl.jsondoc;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;

public class SimpleDocDbSvcTest
{
	@Test
	public void testConstructor ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "db" , new JSONObject () );
		try {
			Assert.assertNotNull ( new SimpleDocDbSvc ( null , jsonObj ) );
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = BuildFailure.class )
	public void testConstructor_Exception () throws BuildFailure
	{
		new SimpleDocDbSvc ( null , new JSONObject () );
	}

	@Test
	public void testGetIdentityDb ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "db" , new JSONObject () );
		try {
			final SimpleDocDbSvc sdds = new SimpleDocDbSvc ( null , jsonObj );
			Assert.assertNotNull ( sdds.getIdentityDb () );;
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testGetAccessDb ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "db" , new JSONObject () );
		try {
			final SimpleDocDbSvc sdds = new SimpleDocDbSvc ( null , jsonObj );
			Assert.assertNotNull ( sdds.getAccessDb () );;
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testGetIdentityManager ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "db" , new JSONObject () );
		try {
			final SimpleDocDbSvc sdds = new SimpleDocDbSvc ( null , jsonObj );
			Assert.assertNotNull ( sdds.getIdentityManager() );;
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testGetAccessManager ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "db" , new JSONObject () );
		try {
			final SimpleDocDbSvc sdds = new SimpleDocDbSvc ( null , jsonObj );
			Assert.assertNotNull ( sdds.getAccessManager () );;
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testGetTagManager ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "db" , new JSONObject () );
		try {
			final SimpleDocDbSvc sdds = new SimpleDocDbSvc ( null , jsonObj );
			Assert.assertNotNull ( sdds.getTagManager () );;
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}
}
