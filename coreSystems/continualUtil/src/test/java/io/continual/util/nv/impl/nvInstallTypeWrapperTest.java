package io.continual.util.nv.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;
import junit.framework.TestCase;

public class nvInstallTypeWrapperTest extends TestCase
{
	// System Properties
	private String oldValueUserName;
	private String oldValueRrInstallation;

	@Before
	protected void setUp () throws Exception
	{
		oldValueUserName = System.getProperty( "user.name" );
		oldValueRrInstallation = System.getProperty( "rr.installation" );
	}

	@Test
	public void testConstructor ()
	{
		System.setProperty( "user.name" , "UserName" );
		System.setProperty( "rr.installation" , "RRInstallation" );
		final nvReadableTable nvrt = new nvReadableTable ();
		nvrt.set( "key1" , "value1" );
		final nvInstallTypeWrapper nvitw = new nvInstallTypeWrapper ( nvrt );
		assertEquals( nvrt.size() , nvitw.size() );
	}

	@Test
	public void testGetAllKeys ()
	{
		final nvReadableTable nvrt = new nvReadableTable ();
		nvrt.set( "key1" , "value1" );
		final nvInstallTypeWrapper nvitw = new nvInstallTypeWrapper ( nvrt );
		assertEquals( nvrt.size() , nvitw.getAllKeys().size() );
	}

	@Test
	public void testGetCopyAsMap ()
	{
		final nvReadableTable nvrt = new nvReadableTable ();
		nvrt.set( "key1" , "value1" );
		final nvInstallTypeWrapper nvitw = new nvInstallTypeWrapper ( nvrt );
		assertEquals( nvrt.size() , nvitw.getCopyAsMap().size() );
	}

	@Test
	public void testHasValueFor ()
	{
		final nvReadableTable nvrt = new nvReadableTable ();
		nvrt.set( "key1" , "value1" );
		final nvInstallTypeWrapper nvitw = new nvInstallTypeWrapper ( nvrt );
		assertTrue( nvitw.hasValueFor( "key1" ) );
	}

	@Test
	public void testGetString ()
	{
		System.setProperty( "user.name" , "UserName" );
		System.setProperty( "rr.installation" , "RRInstallation" );		
		final nvReadableTable nvrt = new nvReadableTable ();
		nvrt.set( "key1" , "value1" );
		final nvInstallTypeWrapper nvitw = new nvInstallTypeWrapper ( nvrt );
		try {
			assertNotNull( nvitw.getString( "key1" ) );
		} catch (MissingReqdSettingException e) {
			fail( "Expected value for the key." );
		}
	}

	@Test
	public void testGetStrings ()
	{
		System.setProperty( "user.name" , "UserName" );
		System.setProperty( "rr.installation" , "RRInstallation" );		
		final nvReadableTable nvrt = new nvReadableTable ();
		nvrt.set( "key1" , "value1" );
		final nvInstallTypeWrapper nvitw = new nvInstallTypeWrapper ( nvrt );
		try {
			assertNotNull( nvitw.getStrings( "key1" ) );
		} catch (MissingReqdSettingException e) {
			fail( "Expected value for the key." );
		}
	}

	@Test
	public void testRescan ()
	{
		System.setProperty( "user.name" , "UserName" );
		System.setProperty( "rr.installation" , "RRInstallation" );		
		final nvReadableTable nvrt = new nvReadableTable ();
		nvrt.set( "key[1]" , "value1" );
		final nvInstallTypeWrapper nvitw = new nvInstallTypeWrapper ( nvrt );

		try {
			nvitw.rescan();
			assertNotNull( nvitw.getString( "key[1]" ) );
		} catch (MissingReqdSettingException e) {
			fail( "Expected value for the key." );
		} catch (LoadException e) {
			fail( "Expected value for the key." );
		}
	}

	@After
	protected void tearDown () throws Exception
	{
		if( oldValueUserName != null )
		{
			System.setProperty( "user.name" , oldValueUserName );
		}
		if( oldValueRrInstallation != null )
		{
			System.setProperty( "rr.installation" , oldValueRrInstallation );
		}
	}
}
