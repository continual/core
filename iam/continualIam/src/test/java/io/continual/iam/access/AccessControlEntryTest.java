package io.continual.iam.access;

import java.util.TreeSet;

import org.junit.Test;

import io.continual.iam.access.AccessControlEntry.Access;
import junit.framework.TestCase;

public class AccessControlEntryTest extends TestCase
{
	@Test
	public void testDefaultAceBuild ()
	{
		try
		{
			AccessControlEntry.builder ().build ();
			fail ();
		}
		catch ( IllegalArgumentException x )
		{
			// default ACL is not enough info
		}
	}

	@Test
	public void testOwnerPermissions ()
	{
		final AccessControlEntry ace = AccessControlEntry.builder ()
			.forOwner().permit().operation ( "baz" ) 
			.build ();
		assertNotNull ( ace );
		assertNotNull ( ace.check ( "ower", new TreeSet<>(), true, "baz" ) );
		assertNull ( ace.check ( "user", new TreeSet<>(), false, "baz" ) );
	}

	@Test
	public void testDenyGroupPermitAllUser ()
	{
		final AccessControlEntry ace = AccessControlEntry.builder ()
			.forSubject("g1").deny().operation ( "op1" )
			.forAllUsers().permit().operation ( "op2" ).build ();
		assertNotNull ( ace );
		final TreeSet<String> groups = new TreeSet<> ();
		groups.add ( "g1" );
		assertNotNull( ace.check ( "u1", groups, false, "op1" ) );
	}

	@Test
	public void testBuilderOperationsCol ()
	{
		final TreeSet<String> ops = new TreeSet<> ();
		ops.add( "ops1" );
		ops.add( "ops2" );

		final AccessControlEntry ace = AccessControlEntry.builder ()
			.forAllUsers ()
			.operations( ops )
			.build ()
		;
		assertEquals ( ops.size(), ace.getOperationCount() );
	}

	@Test
	public void testBuilderOperationsArr ()
	{
		final AccessControlEntry ace = AccessControlEntry.builder ()
			.forAllUsers ()
			.operations( new String[] { "ops1" , "ops2" } )
			.build ()
		;
		assertEquals ( 2 , ace.getOperationCount() );
	}

	@Test
	public void testConstructAce ()
	{
		final AccessControlEntry ace = AccessControlEntry.builder ()
			.operations( new String[] { "ops1" , "ops2" } )
			.forSubject("u1").permit().forAnyOperation()
			.forSubject("u2").permit().operation ( "ops2" )
			.build ()
		;
		final AccessControlEntry result = new AccessControlEntry ( ace );
		assertEquals ( Access.PERMIT , result.check ( "u2" , null , false , "ops2" ) );
	}

	@Test
	public void testConstructUserAcessOper ()
	{
		final AccessControlEntry ace = new AccessControlEntry ( "u1" , Access.PERMIT , "ops1" );
		assertEquals ( Access.PERMIT , ace.check( "u1" , null , false , "ops1" ) );
	}

	@Test
	public void testConstructUserAcessOperArr ()
	{
		final AccessControlEntry ace = new AccessControlEntry ( "u1" , Access.PERMIT , new String[] { "ops1" , "ops2" } );
		assertEquals ( Access.PERMIT , ace.check( "u1" , null , false , "ops2" ) );
	}

	@Test
	public void testClone ()
	{
		final AccessControlEntry ace = new AccessControlEntry ( "u1" , Access.PERMIT , "ops1" );
		final AccessControlEntry result = ace.clone();
		assertEquals ( Access.PERMIT , result.check( "u1" , null , false , "ops1" ) );
	}

	@Test
	public void testGetOperations ()
	{
		final AccessControlEntry ace = new AccessControlEntry ( "u1" , Access.PERMIT , new String[] { "ops1" , "ops2" } );
		assertEquals ( 2 , ace.getOperationSet().size() );
	}

	@Test
	public void testRemoveOperation ()
	{
		final AccessControlEntry ace = new AccessControlEntry ( "u1" , Access.PERMIT , new String[] { "ops1" , "ops2" } );
		assertTrue ( ace.removeOperation( "ops2" ) );
	}

	@Test
	public void testToString ()
	{
		final AccessControlEntry ace = new AccessControlEntry ( "u1" , Access.PERMIT , new String[] { "ops1" , "ops2" } );
		assertNotNull ( ace.toString() );
	}

	@Test
	public void testDeserialize ()
	{
		final AccessControlEntry ace = new AccessControlEntry ( "u1" , Access.PERMIT , new String[] { "ops1" , "ops2" } );
		final AccessControlEntry result = AccessControlEntry.deserialize( ace.serialize() );
		assertNotNull ( result );
		assertEquals ( Access.PERMIT , ace.check( "u1" , null , false , "ops2" ) );
	}
}
