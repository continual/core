package io.continual.iam.access;

import java.util.TreeSet;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.CommonJsonIdentity;
import junit.framework.TestCase;

public class AccessControlListTest extends TestCase
{
	@Test
	public void testDefaultAclBuild ()
	{
		final AccessControlList acl = AccessControlList.builder ()
			.build ()
		;
		assertNotNull ( acl );
		assertNull ( acl.getOwner () );
		assertEquals ( 0, acl.getEntries ().size () );
		assertNull ( acl.getListener () );
	}

	@Test
	public void testOwnerPermissions ()
	{
		final AccessControlList acl = AccessControlList.builder ()
			.ownedBy ( "aaa" )
			.withEntry ( AccessControlEntry.builder ().forAllUsers().permit().operation ( "bar" ).build () )
			.withEntry ( AccessControlEntry.builder ().forOwner().permit().operation ( "baz" ).build () )
			.build ()
		;
		assertNotNull ( acl );
		assertTrue ( acl.canUser ( "aaa", new TreeSet<>(), "bar" ) );
		assertTrue ( acl.canUser ( "aaa", new TreeSet<>(), "bar" ) );
		assertTrue ( acl.canUser ( "aaa", new TreeSet<>(), "baz" ) );
		assertFalse ( acl.canUser ( "bbb", new TreeSet<>(), "baz" ) );
	}

	@Test
	public void testDenyGroupPermitUser ()
	{
		final AccessControlList acl = AccessControlList.builder ()
			.withEntry ( AccessControlEntry.builder ().forSubject("g1").deny().operation ( "op1" ).build () )
			.withEntry ( AccessControlEntry.builder ().forSubject("u1").permit().operation ( "op1" ).build () )
			.build ()
		;
		assertNotNull ( acl );
		final TreeSet<String> groups = new TreeSet<> ();
		groups.add ( "g1" );
		assertFalse ( acl.canUser ( "u1", groups, "op1" ) );
	}

	@Test
	public void testPermitUserDenyGroup ()
	{
		final AccessControlList acl = AccessControlList.builder ()
			.withEntry ( AccessControlEntry.builder ().forSubject("u1").permit().operation ( "op1" ).build () )
			.withEntry ( AccessControlEntry.builder ().forSubject("g1").deny().operation ( "op1" ).build () )
			.build ()
		;
		assertNotNull ( acl );
		final TreeSet<String> groups = new TreeSet<> ();
		groups.add ( "g1" );
		assertTrue ( acl.canUser ( "u1", groups, "op1" ) );
	}

	@Test
	public void testCreateOpenAcl ()
	{
		// all users for any operations
		final AccessControlList acl = AccessControlList.createOpenAcl ();
		assertNotNull ( acl );
		assertTrue ( acl.canUser ( "bbb", new TreeSet<>(), "baz" ) );
	}

	@Test
	public void testConstructor ()
	{
		final AccessControlList acl = AccessControlList.builder ()
				.ownedBy ( "owner" )
				.withEntry ( AccessControlEntry.builder ().forAllUsers().permit().operation ( "ops1" ).build () )
				.withEntry ( AccessControlEntry.builder ().forOwner().permit().operation ( "ops2" ).build () )
				.build ()
			;
		final AccessControlList result = new AccessControlList ( acl );
		assertNotNull ( result );
		assertEquals ( 2 , result.getEntries().size() );
		assertEquals ( "owner" , result.getOwner() );
		assertTrue ( result.canUser ( "bbb", new TreeSet<>(), "ops1" ) );
	}

	@Test
	public void testSetOwner1 ()
	{
		final String expect = "owner";
		final AccessControlList acl = new AccessControlList ();
		final AccessControlList result = acl.setOwner( expect );
		assertNotNull ( acl );
		assertEquals ( expect , result.getOwner() );
	}

	@Test
	public void testSetOwner2 ()
	{
		final String expectOwner = "owner";
		final AccessControlList acl = new AccessControlList ( new TestAclUpdateListener () );
		final AccessControlList result = acl.setOwner( expectOwner );
		assertNotNull ( result );
		assertEquals ( expectOwner , result.getOwner() );
	}

	@Test
	public void testPermit ()
	{
		final AccessControlList acl = new AccessControlList ();
		acl.permit ( "owner" , "ops" );
		assertTrue ( acl.canUser( "owner" , null , "ops" ) );
	}

	@Test
	public void testDeny ()
	{
		final AccessControlList acl = new AccessControlList ();
		acl.deny ( "owner" , "ops" );
		assertFalse ( acl.canUser( "owner" , null , "ops" ) );
	}

	@Test
	public void testClearWithArgs ()
	{
		final AccessControlList acl = AccessControlList.builder ()
			.withEntry ( AccessControlEntry.builder ().forSubject("u1").permit().operation ( "op1" ).build () )
			.withEntry ( AccessControlEntry.builder ().forSubject("g1").deny().operation ( "op1" ).build () )
			.withListener( new TestAclUpdateListener () )
			.build ();
		assertNotNull ( acl );
		acl.clear ( "u1" , "op1" );
		assertEquals ( 1 , acl.getEntries().size() );
	}

	@Test
	public void testClear ()
	{
		final AccessControlList acl = AccessControlList.builder ()
			.withEntry ( AccessControlEntry.builder ().forSubject("u1").permit().operation ( "op1" ).build () )
			.withEntry ( AccessControlEntry.builder ().forSubject("g1").deny().operation ( "op1" ).build () )
			.withListener( new TestAclUpdateListener () )
			.build ();
		assertNotNull ( acl );
		acl.clear();
		assertEquals ( 0 , acl.getEntries().size() );
	}

	@Test
	public void testClearWithoutListener ()
	{
		final AccessControlList acl = AccessControlList.builder ()
			.withEntry ( AccessControlEntry.builder ().forSubject("u1").permit().operation ( "op1" ).build () )
			.withEntry ( AccessControlEntry.builder ().forSubject("g1").deny().operation ( "op1" ).build () )
			.build ();
		assertNotNull ( acl );
		acl.clear();
		assertEquals ( 0 , acl.getEntries().size() );
	}

	@Test
	public void testCanUserIdentity1 ()
	{
		final AccessControlList acl = AccessControlList.builder ()
				.ownedBy ( "owner" )
				.withEntry ( AccessControlEntry.builder ().forAllUsers().permit().operation ( "ops1" ).build () )
				.withEntry ( AccessControlEntry.builder ().forOwner().permit().operation ( "ops2" ).build () )
				.build ();
		final TreeSet<String> groups = new TreeSet<> ();
		groups.add ( "g1" );
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put( "groups" , groups );
		final Identity id = new CommonJsonIdentity ( "user" , jsonObj , null );
		try {
			assertTrue ( acl.canUser( id , "ops1" ) );
		} catch (IamSvcException e) {
			Assert.fail( "Expected to execute. " + e.getMessage() );
		}
	}

	@Test
	public void testCanUserIdentity2 ()
	{
		final AccessControlList acl = AccessControlList.builder ()
				.ownedBy ( "owner" )
				.withEntry ( AccessControlEntry.builder ().forAllUsers().permit().operation ( "ops1" ).build () )
				.withEntry ( AccessControlEntry.builder ().forOwner().permit().operation ( "ops2" ).build () )
				.build ();
		try {
			assertTrue ( acl.canUser( (Identity)null , "ops1" ) );
		} catch (IamSvcException e) {
			Assert.fail( "Expected to execute. " + e.getMessage() );
		}
	}

	@Test
	public void testAddAclEntry ()
	{
		final AccessControlList acl = new AccessControlList ( new TestAclUpdateListener () );
		final AccessControlEntry ace = AccessControlEntry.builder ().forAllUsers().permit().operation ( "ops1" ).build ();
		final AccessControlList result = acl.addAclEntry( ace );
		assertEquals ( 1 , result.getEntries().size() );
	}

	@Test
	public void testAsJson ()
	{
		final AccessControlList acl = AccessControlList.builder ()
			.withEntry ( AccessControlEntry.builder ().forSubject("u1").permit().operation ( "op1" ).build () )
			.withEntry ( AccessControlEntry.builder ().forSubject("g1").deny().operation ( "op1" ).build () )
			.build ();
		assertNotNull ( acl );
		acl.setOwner ( "owner1" );
		final JSONObject result = acl.asJson();
		assertEquals ( "owner1", result.getString("o") );
		assertEquals ( 2 , result.getJSONArray( "e" ).length() );
	}

	@Test
	public void testSerialize ()
	{
		final AccessControlList acl = new AccessControlList ();
		acl.setOwner ( "owner" );
		assertNotNull( acl.serialize() );
	}

	@Test
	public void testDeserializeWithArgs ()
	{
		final AccessControlList acl = new AccessControlList ();
		acl.setOwner ( "owner" );
		assertNotNull ( AccessControlList.deserialize( acl.toString() , null ) );
	}

	@Test
	public void testDeserializeWithArgsJsonObj ()
	{
		final AccessControlList acl = new AccessControlList ();
		acl.setOwner ( "owner" );
		assertNotNull ( AccessControlList.deserialize( (JSONObject)null , null ) );
	}

	private static class TestAclUpdateListener implements AclUpdateListener
	{
		@Override
		public void onAclUpdate ( AccessControlList acli )
		{}
	} 
}
