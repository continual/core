/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package io.continual.iam.impl.jsondoc;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.ApiKey;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.util.time.Clock;
import junit.framework.TestCase;

public class JsonDocDbTest extends TestCase
{
	@Test
	public void testApiKeyCreateOnNullUser () throws IamIdentityDoesNotExist, IamSvcException, IOException
	{
		try ( final JsonDocDb db = new JsonDocDb () )
		{
			
			db.createApiKey ( null );
			fail ( "can't create api key on null user" );
		}
		catch ( IamBadRequestException x )
		{
			// good
		}
	}

	@Test
	public void testApiKeyCreate () throws IamSvcException, IamBadRequestException, IOException
	{
		try ( final JsonDocDb db = new JsonDocDb () )
		{
			final CommonJsonIdentity i = db.createUser ( "test" );
			assertNotNull ( i );
	
			final ApiKey key = db.createApiKey ( i.getId () );
	
			assertNotNull ( key );
			assertNotNull ( key.getKey () );
			assertNotNull ( key.getSecret () );
		}
	}

	@Test
	public void testPasswordReset () throws IamSvcException, IamBadRequestException, IOException
	{
		final Clock.TestClock tc = Clock.useNewTestClock ();

		try ( final JsonDocDb db = new JsonDocDb () )
		{
			final CommonJsonIdentity i = db.createUser ( "test" );
			assertNotNull ( i );
	
			tc.add ( 100 );
	
			String resetTag = i.requestPasswordReset ( 10, "nonce" );
			assertTrue ( db.completePasswordReset ( resetTag, "foobar" ) );
	
			assertNotNull ( db.authenticate ( new UsernamePasswordCredential ( "test", "foobar" ) ) );
	
			resetTag = i.requestPasswordReset ( 10, "nonce" );
			tc.add ( 50000 );
			assertFalse ( db.completePasswordReset ( resetTag, "foobar" ) );
		}
	}

	@Test
	public void testSerialize ()
	{
		try (JsonDocDb jdd = new JsonDocDb ()) {
			assertNotNull ( jdd.serialize() );
		}
	}

	@Test
	public void testGetAllUsers ()
	{
		final JSONObject userJson = new JSONObject ();
		userJson.put ( "userId1" , new JSONObject () );
		final JSONObject usersJsonObj = new JSONObject ();
		usersJsonObj.put ( "users" , userJson );

		final JsonDocDb jdd = new JsonDocDb ( usersJsonObj );
		try {
			assertNotNull ( jdd.getAllUsers () );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
		jdd.close ();
	}

	@Test
	public void testFindUsers ()
	{
		final JSONObject userJson = new JSONObject ();
		userJson.put ( "userId1" , new JSONObject () );
		final JSONObject usersJsonObj = new JSONObject ();
		usersJsonObj.put ( "users" , userJson );

		final JsonDocDb jdd = new JsonDocDb ( usersJsonObj );
		try {
			assertTrue ( jdd.findUsers ( "empty" ).isEmpty () );
			assertFalse ( jdd.findUsers ( "userId" ).isEmpty () );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
		jdd.close ();
	}

	@Test
	public void testLoadAllUsers ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.storeUserObject( "userId1" , new JSONObject () );
			assertFalse ( jdd.loadAllUsers ().isEmpty () );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
		jdd.close ();
	}

	@Test
	public void testGetAllGroups ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.storeGroupObject ( "group1" , new JSONObject () );
			assertFalse ( jdd.getAllGroups ().isEmpty () );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
		jdd.close ();
	}

	@Test
	public void testSweepExpiredTags ()
	{
		final JSONObject byTagValuesInJson = new JSONObject ();
		byTagValuesInJson.put ( JsonDocDb.kExpireEpoch , ( ( Clock.now () - 60000 ) / 1000 ) );
		byTagValuesInJson.put ( JsonDocDb.kTagId , "TagId" );
		byTagValuesInJson.put ( JsonDocDb.kTagType , "TagType" );
		byTagValuesInJson.put ( JsonDocDb.kUserId , "UserId" );

		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.storeTagObject ( "tagid" , "UserId" , "TagType" , byTagValuesInJson );
			jdd.sweepExpiredTags ();
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
		jdd.close ();		
	}

	@Test
	public void testSweepExpiredTags_InvalidTags ()
	{
		final JsonDocDb jdd = new JsonDocDb ( new JSONObject () );
		try {
			jdd.sweepExpiredTags ();
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
		jdd.close ();		
	}

	@Test
	public void testSweepExpiredTags_InvalidByTag ()
	{
		final JSONObject tagsJsonObj = new JSONObject ();
		tagsJsonObj.put ( "tags" , new JSONObject () );
		final JsonDocDb jdd = new JsonDocDb ( tagsJsonObj );
		try {
			jdd.sweepExpiredTags ();
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
		jdd.close ();		
	}

	@Test
	public void testSweepExpiredTags_NotExpired ()
	{
		final JSONObject byTagValuesInJson = new JSONObject ();
		byTagValuesInJson.put ( JsonDocDb.kExpireEpoch , ( ( Clock.now () + 60000 ) / 1000 ) );

		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.storeTagObject ( "tagid" , "UserId" , "TagType" , byTagValuesInJson );
			jdd.sweepExpiredTags ();
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
		jdd.close ();		
	}

	@Test
	public void testDeleteUserObject ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.deleteUserObject ( "UserId" );	// No users
			jdd.storeUserObject ( "UserId" , new JSONObject () );
			jdd.deleteUserObject ( "UserId" );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
		jdd.close ();
	}

	@Test
	public void testCreateNewGroup ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		assertNotNull ( jdd.createNewGroup ( "id" , "group1" ) );
		jdd.close ();
	}

	@Test
	public void testLoadGroupObject ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.loadGroupObject ( "group1" );	// No groups
			jdd.storeGroupObject ( "group1" , new JSONObject () );	// Initializes groups
			jdd.storeGroupObject ( "group2" , new JSONObject () );	// update groups
			jdd.loadGroupObject ( "group1" );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
		jdd.close ();
	}

	@Test
	public void testDeleteGroupObject ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.deleteGroupObject ( "group1" );	// No groups
			jdd.storeGroupObject ( "group1" , new JSONObject () );
			jdd.deleteGroupObject ( "group1" );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
		jdd.close ();
	}

	@Test
	public void testInstantiateGroup ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		assertNotNull ( jdd.instantiateGroup ( "id" , new JSONObject () ) );
		jdd.close ();
	}

	@Test
	public void testLoadApiKeyObject_Null ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			assertNull ( jdd.loadApiKeyObject ( "id" ) );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
		jdd.close ();
	}

	@Test
	public void testStoreApiKeyObject_Exception ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.storeApiKeyObject ( null , new JSONObject () );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} catch (IamBadRequestException e) {
			// Test passed
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testDeleteApiKeyObject ()
	{
		final JSONArray arrJson = new JSONArray();
		arrJson.put ( "UserId1" );	arrJson.put ( "UserId2" );
		final JSONObject userArrJsonObj = new JSONObject ();
		userArrJsonObj.put ( "apiKeys" , arrJson );
		//
		final JSONObject apiKeyInJson = new JSONObject ();
		apiKeyInJson.put ( JsonDocDb.kUserId , "User1" );

		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.storeUserObject ( "User1" , userArrJsonObj );
			jdd.storeApiKeyObject ( "UserId1" , apiKeyInJson );
			jdd.deleteApiKeyObject ( "UserId1" );
		} catch ( IamSvcException | IamBadRequestException e) {
			e.printStackTrace();
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testLoadApiKeysForUser ()
	{
		final JSONObject userArrJsonObj = new JSONObject ();
		final JSONArray arrJson = new JSONArray();
		arrJson.put ( "UserId1" );	arrJson.put ( "UserId2" );
		userArrJsonObj.put ( "apiKeys" , arrJson );

		final JsonDocDb jdd = new JsonDocDb ( new JSONObject () );
		try {
			jdd.storeUserObject( "User1" , userArrJsonObj);
			jdd.loadApiKeysForUser ( "User1" );
		} catch ( IamSvcException | IamIdentityDoesNotExist e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}		
	}

	@Test
	public void testLoadApiKeysForUser_Empty ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.storeUserObject( "User1" , new JSONObject () );
			jdd.loadApiKeysForUser ( "User1" );
		} catch ( IamSvcException | IamIdentityDoesNotExist e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}		
	}

	@Test
	public void testLoadAclObject_Null ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			assertNull ( jdd.loadAclObject ( "acl1" ) );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testLoadAclObject ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.storeAclObject ( "acl1" , new JSONObject () );
			assertNotNull ( jdd.loadAclObject ( "acl1" ) );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testStoreAclObject ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.storeAclObject ( "acl1" , new JSONObject () );	// No acls
			jdd.storeAclObject ( "acl2" , new JSONObject () );	// acls exist
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}		
	}

	@Test
	public void testDeleteAclObject ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.deleteAclObject ( "acl1" );		// No acls
			jdd.storeAclObject ( "acl1" , new JSONObject () );
			jdd.deleteAclObject ( "acl1" );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}		
	}

	@Test
	public void testLoadAliasObject_Null ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			assertNull ( jdd.loadAliasObject ( "alias1" ) );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}		
	}

	@Test
	public void testLoadAliasObject ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( JsonDocDb.kUserId , "User1" );
		final JsonDocDb jdd = new JsonDocDb ( new JSONObject () );
		try {
			jdd.storeUserObject ( "User1" , new JSONObject () );
			jdd.storeAliasObject ( "alias1" , jsonObj );
			assertNotNull ( jdd.loadAliasObject ( "alias1" ) );
		} catch (IamSvcException | IamBadRequestException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testStoreAliasObject ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( JsonDocDb.kUserId , "User1" );
		final JsonDocDb jdd = new JsonDocDb ( new JSONObject () );
		try {
			jdd.storeUserObject ( "User1" , new JSONObject () );
			jdd.storeAliasObject ( "alias1" , jsonObj );
		} catch ( IamSvcException | IamBadRequestException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}
	}

	@Test
	public void testDeleteAliasObject ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( JsonDocDb.kUserId , "User1" );
		final JsonDocDb jdd = new JsonDocDb ( new JSONObject () );
		try {
			jdd.storeUserObject ( "User1" , new JSONObject () );
			jdd.loadUserObject ( "User1" ).put ( "aliases" , new JSONArray () );
			jdd.storeAliasObject ( "alias1" , jsonObj );
			jdd.deleteAliasObject ( "alias1" );
		} catch ( IamSvcException | IamBadRequestException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}		
	}

	@Test
	public void testLoadAliasesForUser ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( JsonDocDb.kUserId , "User1" );
		final JsonDocDb jdd = new JsonDocDb ( new JSONObject () );
		try {
			jdd.storeUserObject ( "User1" , new JSONObject () );
			assertNotNull ( jdd.loadAliasesForUser ( "User1" ) );		// No Aliases
			jdd.loadUserObject ( "User1" ).put ( "aliases" , new JSONArray () );
			assertNotNull ( jdd.loadAliasesForUser ( "User1" ) );		// With Aliases
		} catch ( IamSvcException | IamBadRequestException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}		
	}

	@Test
	public void testLoadAliasesForUser_Exception ()
	{
		final JsonDocDb jdd = new JsonDocDb ( new JSONObject () );
		try {
			jdd.loadAliasesForUser ( "User1" );
		} catch ( IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} catch (IamIdentityDoesNotExist e) {
			// Success
		} finally {
			jdd.close ();
		}		
	}

	@Test
	public void testStoreInvalidJwtToken ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			jdd.storeInvalidJwtToken ( "token1" );	// No tokens
			jdd.storeInvalidJwtToken ( "token2" );
		} catch ( IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}		
	}

	@Test
	public void testIsInvalidJwtToken ()
	{
		final JsonDocDb jdd = new JsonDocDb ();
		try {
			assertFalse ( jdd.isInvalidJwtToken ( "token1" ) );
			jdd.storeInvalidJwtToken ( "token1" );
			assertTrue ( jdd.isInvalidJwtToken ( "token1" ) );
			assertFalse ( jdd.isInvalidJwtToken ( "token2" ) );
		} catch ( IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		} finally {
			jdd.close ();
		}		
	}
}
