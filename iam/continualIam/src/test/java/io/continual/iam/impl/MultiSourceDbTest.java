package io.continual.iam.impl;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamGroupDoesNotExist;
import io.continual.iam.exceptions.IamGroupExists;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamIdentityExists;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.jsondoc.JsonDocDb;
import io.continual.services.ServiceContainer;
import io.continual.util.time.Clock;
import io.continual.iam.access.Resource;
import io.continual.iam.credentials.ApiKeyCredential;
import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.credentials.JwtCredential.InvalidJwtToken;
import io.continual.iam.credentials.UsernamePasswordCredential;

public class MultiSourceDbTest
{
	private JSONObject rawConfig = new JSONObject ();

	@Before
	public void setUp () throws Exception
	{
		final JsonDocDb jdd = new JsonDocDb ();
		final JSONArray arrJson = new JSONArray ();
		arrJson.put ( ( new JSONObject () ).put ( "classname" , jdd.getClass ().getName () ) );
		rawConfig.put ( "dbs" , arrJson );
		jdd.close();
	}

	@After
	public void tearDown ()
	{
		rawConfig = new JSONObject ();
	}

	@Test
	public void testConstructor ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig ); 
			Assert.assertNotNull ( msd );
		} catch ( BuildFailure e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = BuildFailure.class )
	public void testConstructor_Exception () throws BuildFailure
	{
		final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
				new ServiceContainer () , new JSONObject () );
		msd.close ();
	}

	@Test
	public void testUserExists ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.userExists ( "user1" );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testUserOrAliasExists ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.userOrAliasExists ( "user1" );
			msd.close ();
		} catch (BuildFailure | IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testLoadUser ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.loadUser ( "user1" );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testLoadUserOrAlias ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.loadUserOrAlias ( "user1" );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testFindUsers ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			Assert.assertNotNull ( msd.findUsers ( "user" ) );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = IamSvcException.class )
	public void testCreateUser_Exception () throws IamSvcException
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.createUser ( "user1" );
			msd.close ();
		} catch ( IamIdentityExists | BuildFailure e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = IamSvcException.class )
	public void testCreateAnonymousUser_Exception () throws IamSvcException
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.createAnonymousUser ();
			msd.close ();
		} catch ( BuildFailure e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = IamSvcException.class )
	public void testDeleteUser_Exception () throws IamSvcException
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.deleteUser ( "user1" );
			msd.close ();
		} catch ( BuildFailure e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = IamSvcException.class )
	public void testAddAlias_Exception () throws IamSvcException
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.addAlias ( "user1" , "alias1" );
			msd.close ();
		} catch ( BuildFailure | IamBadRequestException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = IamSvcException.class )
	public void testRemoveAlias_Exception () throws IamSvcException
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.removeAlias ( "alias1" );
			msd.close ();
		} catch ( BuildFailure | IamBadRequestException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = IamIdentityDoesNotExist.class )
	public void testGetAliasesFor_Expection () throws IamIdentityDoesNotExist
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.getAliasesFor ( "user1" );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			e.printStackTrace();
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = IamSvcException.class )
	public void testCompletePasswordReset_Exception () throws IamSvcException
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.completePasswordReset ( "tag" , "newpassword" );
			msd.close ();
		} catch ( BuildFailure e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = IamSvcException.class )
	public void testRestoreApiKey_Exception () throws IamSvcException
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.restoreApiKey ( null );
			msd.close ();
		} catch ( BuildFailure | IamBadRequestException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testLoadApiKeyRecord ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			Assert.assertNull ( msd.loadApiKeyRecord ( "apikey" ) );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testAddJwtValidator ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.addJwtValidator ( null );
			msd.close ();
		} catch ( BuildFailure e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void tesGetAllUsers ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			Assert.assertNotNull ( msd.getAllUsers () );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void tesLoadAllUsers ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			Assert.assertNotNull ( msd.loadAllUsers () );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testAuthenticate_UPC ()
	{
		final UsernamePasswordCredential upc = new UsernamePasswordCredential ( "user" , "password" );
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			Assert.assertNull ( msd.authenticate ( upc ) );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testAuthenticate_AKC ()
	{
		final ApiKeyCredential akc = new ApiKeyCredential ( "apikey" , "signedcontent" , "signature" );
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			Assert.assertNull ( msd.authenticate ( akc ) );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testAuthenticate_JwtCred ()
	{
		final long expTime = 1670936933000L , offsetTime = (24 * 60 * 60);		// 1 day
		final String validJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
				"eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA5MzYzMzMsImV4cCI6MTY3MDkzNjkzM30." +
				"1VBwqpRd1UoRsbcabSzMROxa6xucSRghirmQMVHDYRQ";
		try {
			Clock.useNewTestClock ().set ( expTime - offsetTime );
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			Assert.assertNull ( msd.authenticate ( new JwtCredential ( validJwtToken ) ) );
			msd.close ();
		} catch ( BuildFailure | IamSvcException | InvalidJwtToken e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			Clock.useNewTestClock ();
		}
	}

	@Test ( expected = IamSvcException.class )
	public void testCreateJwtToken_Exception () throws IamSvcException
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.createJwtToken ( (Identity) null );
			msd.close ();
		} catch ( BuildFailure e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testInvalidateJwtToken ()
	{
		final String validJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
				"eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA5MzYzMzMsImV4cCI6MTY3MDkzNjkzM30." +
				"1VBwqpRd1UoRsbcabSzMROxa6xucSRghirmQMVHDYRQ";
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.invalidateJwtToken ( validJwtToken );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = IamSvcException.class )
	public void testCreateGroup_Desc () throws IamSvcException
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.createGroup ( "group1" );
			msd.close ();
		} catch ( BuildFailure | IamGroupExists e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test ( expected = IamSvcException.class )
	public void testCreateGroup_IdDesc () throws IamSvcException
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.createGroup ( "groupId" , "groupDesc" );
			msd.close ();
		} catch ( BuildFailure | IamGroupExists e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testGetAclFor ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			Assert.assertNotNull ( msd.getAclFor ( new Resource () { 
				@Override public String getId() {	return "id";}} ) );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testLoadGroup ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			Assert.assertNull ( msd.loadGroup ( "groupid" ) );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void tesGetAllGroups ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			Assert.assertNotNull ( msd.getAllGroups () );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test ( expected = IamGroupDoesNotExist.class )
	public void testGetUsersInGroup () throws IamGroupDoesNotExist
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.getUsersInGroup ( "group1" );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test ( expected = IamIdentityDoesNotExist.class )
	public void testGetUsersnGroup () throws IamIdentityDoesNotExist
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.getUsersGroups ( "user1" );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testgetUserIdForTag ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			Assert.assertNull ( msd.getUserIdForTag ( "tag" ) );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testRemoveMatchingTag ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.removeMatchingTag ( "userid" , "apptagtype" );
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testSweepExpiredTags ()
	{
		try {
			final MultiSourceDb<Identity, Group> msd = new MultiSourceDb<Identity , Group> ( 
					new ServiceContainer () , rawConfig );
			msd.sweepExpiredTags ();
			msd.close ();
		} catch ( BuildFailure | IamSvcException e ) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}
}
