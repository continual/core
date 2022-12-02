package io.continual.util.nv.impl;

import java.util.Properties;

import org.junit.Test;

import junit.framework.TestCase;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class nvReadableStackTest extends TestCase
{
	@SuppressWarnings("serial")
	private final Properties props = new Properties () {{
		put( "timeStart" , "-604800000" );	put( "timeScale" , "900" );
	}};

	@Test
	public void testConstructorNoArgs ()
	{
		final nvReadableStack nvrs = new nvReadableStack ();
		assertEquals( 0 , nvrs.size() );
	}

	@Test
	public void testPush ()
	{
		final nvReadableTable nvrt = new nvReadableTable( props );

		final nvReadableStack nvrs = new nvReadableStack ();
		nvrs.push( nvrt );
		assertEquals( props.size() , nvrs.size() );
	}

	@Test
	public void testPushBelow ()
	{
		final nvReadableTable nvrt1 = new nvReadableTable( props );
		final nvReadableTable nvrt2 = new nvReadableTable();
		nvrt2.set( "key1" , "value1" );

		final nvReadableStack nvrs = new nvReadableStack ();
		nvReadableStack result = nvrs.pushBelow( nvrt1 , nvrt2 );	// if condition
		assertEquals( props.size() , result.size() );

		result = nvrs.pushBelow( nvrt2 , nvrt1 );	// else condition
		assertEquals( ( props.size() + 1 ) , result.size() );
	}

	@Test
	public void testGetString ()
	{
		try
		{
			final nvReadableTable nvrt = new nvReadableTable( props );
			final nvReadableStack nvrs = new nvReadableStack ();
			nvrs.push( nvrt );
	
			assertEquals( (String)props.get( "timeStart" ) , nvrs.getString( "timeStart" ) );
		}
		catch(MissingReqdSettingException mrse)
		{
			fail( "Expected property does not exist." );
		}
	}

	@Test
	public void testGetStrings ()
	{
		try
		{
			final nvReadableTable nvrt = new nvReadableTable( props );
			final nvReadableStack nvrs = new nvReadableStack ();
			nvrs.push( nvrt );
	
			assertNotNull( nvrs.getStrings( "timeStart" ) );
		}
		catch(MissingReqdSettingException mrse)
		{
			fail( "Expected property does not exist." );
		}
	}

	@Test
	public void testHasValueFor ()
	{
		final nvReadableTable nvrt1 = new nvReadableTable( props );
		final nvReadableTable nvrt2 = new nvReadableTable();
		nvrt2.set( "key1" , "value1" );		
		final nvReadableStack nvrs = new nvReadableStack ();
		nvrs.push( nvrt1 );
		nvrs.push( nvrt2 );
		assertTrue( nvrs.hasValueFor( "timeStart" ) );
	}

	@Test
	public void testRescan ()
	{
		final nvReadableTable nvrt = new nvReadableTable( props );
		final nvReadableStack nvrs = new nvReadableStack ();
		nvrs.push( nvrt );
		try {
			nvrs.rescan();
		} catch( LoadException le ) {
			fail( "Failed during rescan" );
		}
	}

	@Test
	public void testGetAllKeys ()
	{
		final nvReadableTable nvrt = new nvReadableTable( props );
		final nvReadableStack nvrs = new nvReadableStack ();
		nvrs.push( nvrt );
		assertEquals( props.size() , nvrs.getAllKeys().size() );
	}

	@Test
	public void testCopyAsMap ()
	{
		final nvReadableTable nvrt = new nvReadableTable( props );
		final nvReadableStack nvrs = new nvReadableStack ();
		nvrs.push( nvrt );
		assertEquals( nvrt.size() , nvrs.getCopyAsMap().size() );
	}
}
