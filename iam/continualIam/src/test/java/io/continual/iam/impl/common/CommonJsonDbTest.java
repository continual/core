package io.continual.iam.impl.common;

import org.junit.Assert;
import org.junit.Test;

import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamGroupDoesNotExist;
import io.continual.iam.exceptions.IamGroupExists;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamIdentityExists;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.JwtValidator;
import io.continual.iam.impl.jsondoc.JsonDocDb;
import io.continual.util.data.Sha1HmacSigner;
import io.continual.util.time.Clock;
import io.continual.iam.access.AccessControlEntry;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.ProtectedResource;
import io.continual.iam.access.Resource;
import io.continual.iam.credentials.ApiKeyCredential;
import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.credentials.JwtCredential.InvalidJwtToken;
import io.continual.iam.access.AccessControlEntry.Access;

public class CommonJsonDbTest
{
	@Test
	public void testPopulateMetrics ()
	{
		try ( JsonDocDb jdd = new JsonDocDb () ) {
			jdd.populateMetrics ( null );	// Empty method
		}
	}

	@Test
	public void testUserExists ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			Assert.assertFalse ( jdd.userExists ( null ) );
			jdd.createUser ( "userid" );
			Assert.assertTrue ( jdd.userExists ( "userid" ) );
		} catch (IamSvcException | IamIdentityExists e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testUserOrAliasExists ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			Assert.assertFalse ( jdd.userOrAliasExists ( null ) );
			jdd.createUser ( "userid" );
			Assert.assertTrue ( jdd.userOrAliasExists ( "userid" ) );
			jdd.addAlias ( "userid" , "aliasid" );
			Assert.assertTrue ( jdd.userOrAliasExists ( "aliasid" ) );
		} catch (IamSvcException | IamBadRequestException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}		
	}

	@Test
	public void testLoadUserOrAlias ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			Assert.assertNull ( jdd.loadUserOrAlias ( null ) );
			jdd.createUser ( "userid" );
			Assert.assertNotNull ( jdd.loadUserOrAlias ( "userid" ) );
			jdd.addAlias ( "userid" , "aliasid" );
			Assert.assertNotNull ( jdd.loadUserOrAlias ( "aliasid" ) );
		} catch (IamSvcException | IamBadRequestException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}		
	}

	@Test ( expected = IamIdentityExists.class )
	public void testCreateUser_Exception () throws IamIdentityExists
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.createUser ( "userid" );
			jdd.createUser ( "userid" );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}		
	}

	@Test
	public void testCreateAnonymousUser ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.createAnonymousUser ();
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}		
	}

	@Test
	public void testDeleteUser ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.createUser ( "userid" );
			jdd.deleteUser ( "userid" );
		} catch (IamSvcException | IamIdentityExists e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testAddUserToGroup ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.createUser ( "user1" );		//	New User
			jdd.createGroup ( "group1" , "group1" );	//	New Group
			jdd.addUserToGroup ( "group1" , "user1" );
			Assert.assertEquals ( 1 , jdd.getUsersGroups ( "user1" ).size () );
		} catch (IamSvcException | IamIdentityExists | IamIdentityDoesNotExist | IamGroupDoesNotExist | IamGroupExists e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testRemoveUserFromGroup ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.createUser ( "user1" );		//	New User
			jdd.createGroup ( "group1" , "group1" );	//	New Group
			jdd.addUserToGroup ( "group1" , "user1" );
			jdd.removeUserFromGroup ( "group1" , "user1" );
			Assert.assertEquals ( 0 , jdd.getUsersInGroup ( "group1" ).size () );
		} catch (IamSvcException | IamIdentityExists | IamIdentityDoesNotExist | IamGroupDoesNotExist | IamGroupExists e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testGetAclFor ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			// Protected Resource
			Assert.assertNotNull ( jdd.getAclFor ( new ProtectedResource () {
				@Override public String getId() {	return "id";	}
				@Override public AccessControlList getAccessControlList() {	return new AccessControlList ();	}
			} ) );
			// Resource
			final AccessControlList acl = jdd.getAclFor ( new Resource () {
				@Override public String getId() {	return "id";	}
			} );
			Assert.assertNotNull( acl );
			acl.addAclEntry ( new AccessControlEntry ( "user1" , Access.PERMIT , "ops1" ) );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testOnAclUpdate ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		jdd.onAclUpdate ( null );	//	Empty method
		jdd.close ();
	}

	@Test ( expected = IamSvcException.class )
	public void testCanUser () throws IamSvcException 
	{
		try (JsonDocDb jdd = new JsonDocDb ()) {
			jdd.canUser ( "userid" , new Resource () {	@Override public String getId() { return "id"; } } , "ops1" );
		}
	}

	@Test
	public void testRemoveAlias ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.createUser ( "user1" );
			jdd.addAlias ( "user1" , "alias1" );
			jdd.removeAlias ( "alias1" );
			Assert.assertEquals ( 0 , jdd.getAliasesFor ( "user1" ).size () );
		} catch (IamSvcException | IamBadRequestException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testAddJwtValidator ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		jdd.addJwtValidator ( new JwtValidator () { @Override 
			public boolean validate(JwtCredential jwt) throws IamSvcException {
				return false;
			} } );
		jdd.close ();
	}

	@Test
	public void testInvalidateJwtToken ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.invalidateJwtToken ( "token" );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testLoadApiKeyRecord ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			Assert.assertNull (jdd.loadApiKeyRecord ( "apikey" ) );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testRestoreApiKey ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.createUser ( "user1" );
			jdd.restoreApiKey ( new CommonJsonApiKey ( "apikey" , 
					CommonJsonApiKey.initialize ( "secret" , "user1" ) ) );
			Assert.assertNotNull ( jdd.loadApiKeyRecord ( "apikey" ) );
		} catch (IamSvcException | IamBadRequestException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test ( expected = IamBadRequestException.class )
	public void testRestoreApiKey_Exception () throws IamBadRequestException
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.createUser ( "user1" );
			jdd.restoreApiKey ( new CommonJsonApiKey ( "apikey" , 
					CommonJsonApiKey.initialize ( "secret" , "user1" ) ) );
			jdd.restoreApiKey ( new CommonJsonApiKey ( "apikey" , 
					CommonJsonApiKey.initialize ( "secret" , "user1" ) ) );
		} catch (IamSvcException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test ( expected = IamSvcException.class )
	public void testCreateJwtToken_Exception () throws IamSvcException
	{
		final JsonDocDb jdd = new JsonDocDb ();
		jdd.createJwtToken ( null );
		jdd.close ();
	}

	@Test
	public void testAuthenticate_AKC ()
	{
		final String signature = Sha1HmacSigner.sign ( "signedcontent" , "secret" );
		final ApiKeyCredential akc = new ApiKeyCredential ( "apikey" , "signedcontent" , signature ); 
		
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			Assert.assertNull ( jdd.authenticate ( akc ) );
			jdd.createUser ( "user1" );
			jdd.restoreApiKey ( new CommonJsonApiKey ( "apikey" , 
					CommonJsonApiKey.initialize ( "secret" , "user1" ) ) );
			jdd.authenticate ( akc );
		} catch (IamSvcException | IamBadRequestException e) {
			e.printStackTrace();
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testAuthenticate_JWT ()
	{
		final long expTime = 1670936933000L , offsetTime = (24 * 60 * 60);		// 1 day
		final String validJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
				"eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA5MzYzMzMsImV4cCI6MTY3MDkzNjkzM30." +
				"1VBwqpRd1UoRsbcabSzMROxa6xucSRghirmQMVHDYRQ";
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			Assert.assertNull ( jdd.authenticate ( (JwtCredential) null ) );
			Clock.useNewTestClock ().set ( expTime - offsetTime );
			jdd.addJwtValidator ( new JwtValidator () { @Override 
				public boolean validate(JwtCredential jwt) throws IamSvcException {
					return true;
				} } );
			jdd.authenticate ( new JwtCredential ( validJwtToken ) );
		} catch (IamSvcException | InvalidJwtToken e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
			Clock.useNewTestClock ();
		}
	}
}
