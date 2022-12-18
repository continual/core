package io.continual.iam.impl;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.jsondoc.JsonDocDb;
import io.continual.services.Service.FailedToStart;
import io.continual.services.ServiceContainer;

public class BasicIamServiceWrapperTest
{
	@SuppressWarnings("resource")
	@Test
	public void testConstructor ()
	{
		final JSONObject buildJson = new JSONObject ();
		buildJson.put ( "classname" , new JsonDocDb ().getClass ().getName () );
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "db" , buildJson );
		try {
			final BasicIamServiceWrapper<Identity, Group> bisw = new BasicIamServiceWrapper<Identity, Group> 
					( new ServiceContainer () , jsonObj ); 
			Assert.assertNotNull ( bisw );
			Assert.assertNotNull ( bisw.getIdentityDb () );
			Assert.assertNotNull ( bisw.getAccessDb () );
			Assert.assertNotNull ( bisw.getIdentityManager () );
			Assert.assertNotNull ( bisw.getAccessManager () );
			Assert.assertNotNull ( bisw.getTagManager () );
		} catch (IamSvcException | BuildFailure e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@SuppressWarnings("resource")
	@Test
	public void testOverrideMethods ()
	{
		final JSONObject buildJson = new JSONObject ();
		buildJson.put ( "classname" , new JsonDocDb ().getClass ().getName () );
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "db" , buildJson );
		try {
			final BasicIamServiceWrapper<Identity, Group> bisw = new BasicIamServiceWrapper<Identity, Group> 
					( new ServiceContainer () , jsonObj );
			bisw.onStartRequested ();
			bisw.onStopRequested ();
			bisw.populateMetrics ( null );
		} catch (IamSvcException | BuildFailure | FailedToStart e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}
}
