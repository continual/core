package io.continual.iam.impl.common;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.impl.jsondoc.JsonDocDb;
import junit.framework.TestCase;

public class CommonJsonGroupTest extends TestCase
{
	private JSONObject groupData = new JSONObject ();

	@Before
	public void setUp ()
	{
		final JSONArray jsonArr = new JSONArray ();
		jsonArr.put ( "mem1" );	jsonArr.put ( "mem2" );
		groupData.put ( "members" , jsonArr );
		groupData.put ( "name" , "continual" );
	}

	@After
	public void tearDown ()
	{
		groupData = null;
	}

	@Test
	public void testInitializeGroup ()
	{
		final JSONObject jsonObj = CommonJsonGroup.initializeGroup ( "continual" );
		assertNotNull ( jsonObj );
		assertEquals ( "continual" , jsonObj.get ( "name" ) );
	}

	@Test
	public void testGetId ()
	{
		final CommonJsonGroup cjg = new CommonJsonGroup ( "id" , groupData , null );
		assertEquals ( "id" , cjg.getId () );
	}

	@Test
	public void testGetName ()
	{
		final CommonJsonGroup cjg = new CommonJsonGroup ( "id" , groupData , null );
		assertEquals ( "continual" , cjg.getName () );
	}

	@Test
	public void testGetMembers ()
	{
		final CommonJsonGroup cjg = new CommonJsonGroup ( "id" , groupData , null );
		try {
			assertTrue ( cjg.getMembers ().contains ( "mem1" ) );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testParse ()
	{
		final CommonJsonGroup cjg = new CommonJsonGroup ( "id" , groupData , null );
		try {
			assertTrue ( cjg.isMember ( "mem1" ) );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testAddUser1 ()
	{
		final CommonJsonGroup cjg = new CommonJsonGroup ( "id" , groupData , new JsonDocDb () );
		try {
			cjg.addUser ( "user1" );
			assertTrue ( cjg.isMember ( "user1" ) );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testAddUser2 ()
	{
		final CommonJsonGroup cjg = new CommonJsonGroup ( "id" , groupData , new JsonDocDb () );
		try {
			cjg.addUser ( "mem1" );		// Already existing user
			assertTrue ( cjg.isMember ( "mem1" ) );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	@Test
	public void testRemoveUser1 ()
	{
		final CommonJsonGroup cjg = new CommonJsonGroup ( "id" , groupData , new JsonDocDb () );
		try {
			cjg.removeUser ( "mem1" );	// Already existing user
			assertFalse ( cjg.isMember ( "mem1" ) );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testRemoveUser2 ()
	{
		final CommonJsonGroup cjg = new CommonJsonGroup ( "id" , groupData , new JsonDocDb () );
		try {
			cjg.removeUser ( "user1" );		// Non existing user
			assertFalse ( cjg.isMember ( "user1" ) );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}		
	}

	@Test
	public void testReload ()
	{
		final CommonJsonGroup cjg = new CommonJsonGroup ( "id" , groupData , new JsonDocDb () );
		try {
			cjg.addUser ( "user1" );
			cjg.reload ();
			assertNotNull ( cjg.getDataRecord () );
		} catch (IamSvcException e) {
			fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	// Deprecated method on removal test case to be removed
	@SuppressWarnings("deprecation")
	@Test
	public void testConstructor ()
	{
		assertNotNull ( new CommonJsonGroup ( null , "id" , groupData ) );
	}
}
