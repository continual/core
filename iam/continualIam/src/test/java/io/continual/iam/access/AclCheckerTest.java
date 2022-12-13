package io.continual.iam.access;

import java.util.TreeSet;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.CommonJsonIdentity;

public class AclCheckerTest
{
	private Identity id;

	@Before
	public void setUp ()
	{
		final TreeSet<String> groups = new TreeSet<> ();
		groups.add ( "g1" );
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put( "groups" , groups );
		id = new CommonJsonIdentity ( "owner" , jsonObj , null );
	}

	@Test
	public void testForUser ()
	{
		final AclChecker aclc = new AclChecker ();
		final AclChecker result = aclc.forUser ( null );
		Assert.assertNotNull ( result );
	}

	@Test
	public void testReading ()
	{
		final AccessControlList acl = AccessControlList.builder ()
				.ownedBy ( "owner" )
				.withEntry ( AccessControlEntry.builder ().forOwner().permit().operation ( AccessControlList.READ ).build () )
				.build ();
		final AclChecker aclc = new AclChecker ();
		aclc.forUser ( id );
		aclc.reading ();
		final AclChecker result = aclc.controlledByAcl( acl );
		try {
			result.check ();
		} catch (AccessException | IamSvcException e) {
			Assert.fail ( "Expected to execute. " + e.getMessage() );
		}
	}

	@Test
	public void testUpdating ()
	{
		final AccessControlList acl = AccessControlList.builder ()
				.ownedBy ( "owner" )
				.withEntry ( AccessControlEntry.builder ().forOwner().permit().operation ( AccessControlList.UPDATE ).build () )
				.build ();
		final AclChecker aclc = new AclChecker ();
		aclc.forUser ( id );
		aclc.updating ();
		final AclChecker result = aclc.controlledByAcl( acl );
		try {
			result.check ();
		} catch (AccessException | IamSvcException e) {
			Assert.fail ( "Expected to execute. " + e.getMessage() );
		}
	}

	@Test
	public void testCreating ()
	{
		final AccessControlList acl = AccessControlList.builder ()
				.ownedBy ( "owner" )
				.withEntry ( AccessControlEntry.builder ().forOwner().permit().operation ( AccessControlList.CREATE ).build () )
				.build ();
		final AclChecker aclc = new AclChecker ();
		aclc.forUser ( id );
		aclc.creating ();
		final AclChecker result = aclc.controlledByAcl( acl );
		try {
			result.check ();
		} catch (AccessException | IamSvcException e) {
			Assert.fail ( "Expected to execute. " + e.getMessage() );
		}
	}

	@Test
	public void testDeleting ()
	{
		final AccessControlList acl = AccessControlList.builder ()
				.ownedBy ( "owner" )
				.withEntry ( AccessControlEntry.builder ().forOwner().permit().operation ( AccessControlList.DELETE ).build () )
				.build ();
		final AclChecker aclc = new AclChecker ();
		aclc.forUser ( id );
		aclc.deleting ();
		final AclChecker result = aclc.controlledByAcl( acl );
		try {
			result.check ();
		} catch (AccessException | IamSvcException e) {
			Assert.fail ( "Expected to execute. " + e.getMessage() );
		}
	}

	@Rule
	public ExpectedException exceptionRule = ExpectedException.none();

	@Test
	public void testOnResource () throws AccessException
	{
		exceptionRule.expect ( AccessException.class );
		exceptionRule.expectMessage ( id.getId() + " may not " + AccessControlList.UPDATE.toLowerCase() + " resid" );
		final AccessControlList acl = AccessControlList.builder ()
				.ownedBy ( "owner" )
				.withEntry ( AccessControlEntry.builder ().forOwner().permit().operation ( AccessControlList.READ ).build () )
				.build ();
		final AclChecker aclc = new AclChecker ();
		aclc.forUser ( id );
		aclc.updating ();
		aclc.onResource( "resid" );
		final AclChecker result = aclc.controlledByAcl( acl );
		try {
			result.check();
		} catch (IamSvcException e) {
			Assert.fail( "Expected to execute. " + e.getMessage() );
		}
	}

	@Test
	public void testCheckNoAcl () throws AccessException
	{
		exceptionRule.expect ( AccessException.class );
		exceptionRule.expectMessage ( "No ACL provided." );
		final AclChecker aclc = new AclChecker ();
		try {
			aclc.check();
		} catch (IamSvcException e) {
			Assert.fail( "Expected to execute. " + e.getMessage() );
		}
	}

	@Test
	public void testCheckNoOps () throws AccessException
	{
		exceptionRule.expect ( AccessException.class );
		exceptionRule.expectMessage ( "No operation provided." );
		final AclChecker aclc = new AclChecker ();
		aclc.controlledByAcl( new AccessControlList () );
		try {
			aclc.check();
		} catch (IamSvcException e) {
			Assert.fail( "Expected to execute. " + e.getMessage() );
		}
	}

	@After
	public void tearDown ()
	{
		id = null;
	}
}
