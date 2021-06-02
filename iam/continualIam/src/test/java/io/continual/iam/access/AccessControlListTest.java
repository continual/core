package io.continual.iam.access;

import java.util.TreeSet;

import org.junit.Test;

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
}
