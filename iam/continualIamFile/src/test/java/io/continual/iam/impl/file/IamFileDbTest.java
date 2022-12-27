package io.continual.iam.impl.file;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.access.AclUpdateListener;
import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamGroupExists;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamIdentityExists;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.impl.common.jwt.JwtProducer;
import io.continual.iam.impl.common.CommonJsonDb.AclFactory;
import io.continual.util.time.Clock;

public class IamFileDbTest
{
	@Test ( expected = IamSvcException.class )
	public void testConstructor_Exception () throws IamSvcException 
	{
		try {
			new IamFileDb.Builder ()
					.usingFile ( new File (File.separator + "tmp" + File.separator + "temp.txt") )
					.withPassword ( "password" )
					.usingAclFactory ( new AclFactory () {
						@Override public AccessControlList createDefaultAcl(AclUpdateListener acll) {
							return AccessControlList.initialize ( acll );
						} 
					})
					.withJwtProducer ( new JwtProducer.Builder ().withIssuerName ( "issuer" )
							.usingSigningKey ( "12345" ).build () )
					.readonly ()
					.forceInit ()
					.build ()
				;
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test
	public void testLoadAllUsers ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			fDb.createUser ( "user1" );
			Assert.assertEquals ( 1 , fDb.loadAllUsers ().size () );
			fDb.close ();
		} catch (IamSvcException | IOException | IamIdentityExists e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test
	public void testFindUsers ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			fDb.createUser ( "userId1" );
			fDb.createUser ( "user2" );
			Assert.assertEquals ( 1 , fDb.findUsers ( "userId" ).size () );
			fDb.close ();
		} catch (IamSvcException | IOException | IamIdentityExists e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test
	public void testDeleteUser ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			fDb.deleteUser ( "user1" );
			fDb.createUser ( "user1" );
			Assert.assertEquals ( 1 , fDb.loadAllUsers ().size () );
			fDb.deleteUser ( "user1" );
			Assert.assertTrue ( fDb.loadAllUsers ().isEmpty () );
			fDb.close ();
		} catch (IamSvcException | IamIdentityExists | IOException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test
	public void testGetAllGroups ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			fDb.createGroup ( "GroupId" , "GroupDesc" );
			Assert.assertEquals ( 1 , fDb.getAllGroups ().size () );
			fDb.close ();
		} catch (IamSvcException | IOException | IamGroupExists e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test
	public void testDeleteGroupObject ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			fDb.createGroup ( "GroupId" , "GroupDesc" );
			Assert.assertEquals ( 1 , fDb.getAllGroups ().size () );
			fDb.deleteGroupObject ( "GroupId" );
			Assert.assertEquals ( 0 , fDb.getAllGroups ().size () );
			fDb.close ();
		} catch (IamSvcException | IOException | IamGroupExists e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test
	public void testSweepExpiredTags ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			Clock.useNewTestClock ().set ( System.currentTimeMillis() );
			Clock.useNewTestClock ().add ( -60 , TimeUnit.MINUTES );
			fDb.createTag ( "userid" , "appTagType" , 30 , TimeUnit.MINUTES , "nonce" );
			Clock.useNewTestClock ().set ( System.currentTimeMillis() );
			fDb.sweepExpiredTags ();
			fDb.close ();
		} catch (IamSvcException | IOException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}		
	}

	@Test
	public void testCreateApiKeyObject ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			Assert.assertNotNull ( fDb.createApiKeyObject ( "userid" , "apikey" , "apisecret" ) );
			fDb.close ();
		} catch (IamSvcException | IOException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test
	public void testStoreApiKeyObject ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			fDb.createUser ( "userid" );
			final JSONObject jsonObj = fDb.createApiKeyObject ( "userid" , "apikey" , "apisecret" );
			fDb.storeApiKeyObject ( "apikey" , jsonObj );
			Assert.assertNotNull ( fDb.loadApiKeyObject ( "apikey" ) );
			fDb.close ();
		} catch (IamSvcException | IOException | IamBadRequestException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test ( expected = IamBadRequestException.class )
	public void testStoreApiKeyObject_Exception () throws IamBadRequestException
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			fDb.storeApiKeyObject ( "apikey" , new JSONObject () );
			fDb.close ();
		} catch (IamSvcException | IOException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test
	public void testDeleteApiKeyObject ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			fDb.createUser ( "userid" );
			final JSONObject jsonObj = fDb.createApiKeyObject ( "userid" , "apikey" , "apisecret" );
			fDb.storeApiKeyObject ( "apikey" , jsonObj );
			fDb.deleteApiKeyObject ( "apikey" );
			Assert.assertNull ( fDb.loadApiKeyObject ( "apikey" ) );
			fDb.close ();
		} catch (IamSvcException | IOException | IamBadRequestException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test
	public void testInstantiateApiKey ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			fDb.createUser ( "userid" );
			final JSONObject jsonObj = fDb.createApiKeyObject ( "userid" , "apikey" , "apisecret" );
			Assert.assertNotNull ( fDb.instantiateApiKey ( "apikey" , jsonObj ) );
			fDb.close ();
		} catch (IamSvcException | IOException | IamBadRequestException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test
	public void testLoadApiKeysForUser ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			fDb.createUser ( "userid" );
			Assert.assertTrue ( fDb.loadApiKeysForUser ( "userid" ).isEmpty () );
			final JSONObject jsonObj = fDb.createApiKeyObject ( "userid" , "apikey" , "apisecret" );
			fDb.storeApiKeyObject ( "apikey" , jsonObj );
			Assert.assertFalse ( fDb.loadApiKeysForUser ( "userid" ).isEmpty () );
			fDb.close ();
		} catch (IamSvcException | IOException | IamBadRequestException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test ( expected = IamIdentityDoesNotExist.class )
	public void testLoadApiKeysForUser_Exception () throws IamIdentityDoesNotExist
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			fDb.loadApiKeysForUser ( "userid" );
			fDb.close ();
		} catch (IamSvcException | IOException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test
	public void testAclObject ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			fDb.storeAclObject ( "aclid" , new JSONObject () );
			Assert.assertNotNull ( fDb.loadAclObject ( "aclid" ) );
			fDb.deleteAclObject ( "aclid" );
			Assert.assertNull ( fDb.loadAclObject ( "aclid" ) );
			fDb.close ();
		} catch (IamSvcException | IOException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}
	}

	@Test
	public void testLoadTagObject ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			Clock.useNewTestClock ().set ( System.currentTimeMillis() );
			Clock.useNewTestClock ().add ( -60 , TimeUnit.MINUTES );
			fDb.createTag ( "userid" , "apptagtype" , 30 , TimeUnit.MINUTES , "nonce" );
			Assert.assertNotNull ( fDb.loadTagObject ( "userid" , "apptagtype" , false ) );
			Clock.useNewTestClock ().set ( System.currentTimeMillis() );
			Assert.assertNull ( fDb.loadTagObject ( "userid" , "apptagtype" , false ) );
			fDb.close ();
		} catch (IamSvcException | IOException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}		
	}

	@Test
	public void testAliasObject ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			fDb.createUser ( "userid" );
			Assert.assertTrue ( fDb.loadAliasesForUser ( "userid" ).isEmpty () );

			fDb.addAlias ( "userid" , "aliasid" );
			Assert.assertNotNull ( fDb.loadAliasObject ( "aliasid" ) );
			Assert.assertFalse ( fDb.loadAliasesForUser ( "userid" ).isEmpty () );

			fDb.deleteAliasObject ( "aliasid" );
			Assert.assertNull ( fDb.loadAliasObject ( "aliasid" ) );
			fDb.close ();
		} catch (IamSvcException | IOException | IamBadRequestException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}		
	}

	@Test ( expected = IamIdentityDoesNotExist.class )
	public void testLoadAliasForUser_Exception () throws IamIdentityDoesNotExist
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			fDb.loadAliasesForUser ( "userid" );
			fDb.close ();
		} catch (IamSvcException | IOException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}		
	}

	@Test
	public void testInvalidJwtToken ()
	{
		try {
			final IamFileDb fDb = new IamFileDb.Builder ()
					.usingFile ( File.createTempFile ( "iamUnitTest", ".db" ) )
					.forWrites ()
					.forceInit ()
					.build ()
				;
			final String invalidTypJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkFCQyJ9.eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA5MzYzMzMsImV4cCI6MTY3MDkzNjkzM30.xpwXEnKFznVLo9nNkVFOOzRGW1fno-BRzwFXHFi19p8";
			fDb.storeInvalidJwtToken ( invalidTypJwtToken );
			Assert.assertTrue ( fDb.isInvalidJwtToken ( invalidTypJwtToken ) );
			Assert.assertFalse ( fDb.isInvalidJwtToken ( "token" ) );
			fDb.close ();
		} catch (IamSvcException | IOException e) {
			Assert.fail ( "Expected to execute but failed with exception. " + e.getMessage () );
		}		
	}
}
