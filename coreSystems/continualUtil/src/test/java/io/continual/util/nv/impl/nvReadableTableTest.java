package io.continual.util.nv.impl;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import org.junit.Test;

import io.continual.util.nv.NvReadable.MissingReqdSettingException;
import junit.framework.TestCase;

public class nvReadableTableTest extends TestCase
{
	@SuppressWarnings("serial")
	private final Map<String , String> hmKeyValue = new HashMap<String , String> () {{
		put( "timeStart" , "-604800000" );	put( "timeScale" , "900" );
	}};
	@SuppressWarnings("serial")
	private final Properties props = new Properties () {{
		put( "timeStart" , "-604800000" );	put( "timeScale" , "900" );
	}};

	@Test
	public void testSet ()
	{
		final nvReadableTable nvrt = new nvReadableTable ();
		// Set map as parameter
		nvrt.set( hmKeyValue );
		assertEquals( hmKeyValue.size() , nvrt.size() );

		// Set key , value as parameters
		nvrt.set( "key1" , "value1" );
		assertEquals( ( hmKeyValue.size() + 1 ) , nvrt.size() );
	}

	@Test
	public void testConstructorMapParams ()
	{
		// Null map as parameter
		final nvReadableTable nvrt1 = new nvReadableTable ( ( Map<String , String> )null );
		assertEquals( 0 , nvrt1.size() );

		// Map with values as parameter
		final nvReadableTable nvrt2 = new nvReadableTable ( hmKeyValue );
		assertEquals( hmKeyValue.size() , nvrt2.size() );
	}

	@Test
	public void testConstructorPropertyParams ()
	{
		final nvReadableTable nvrt = new nvReadableTable ( props );
		assertEquals( props.size() , nvrt.size() );
	}

	@Test
	public void testClear ()
	{
		final nvReadableTable nvrt = new nvReadableTable ( props );
		// Clear by key
		nvrt.clear( "timeStart" );
		assertEquals( ( props.size() - 1 ) , nvrt.size() );

		// Clear all
		nvrt.clear();
		assertEquals( 0 , nvrt.size() );
	}

	@Test
	public void testHasValueFor ()
	{
		final nvReadableTable nvrt = new nvReadableTable ( props );
		assertTrue( nvrt.hasValueFor( "timeStart") );
	}

	@Test
	public void testGetString ()
	{
		try
		{
			final nvReadableTable nvrt = new nvReadableTable ( props );
			assertEquals( props.get( "timeStart" ) , nvrt.getString( "timeStart" ) );
		}
		catch( MissingReqdSettingException mrse )
		{
			fail( "Expected property does not exist." );
		}
	}

	@Test
	public void testGetStrings ()
	{
		try
		{
			final nvReadableTable nvrt = new nvReadableTable ( props );
			assertNotNull( nvrt.getStrings( "timeStart" ) );
		}
		catch( MissingReqdSettingException mrse )
		{
			fail( "Expected property does not exist." );
		}
	}

	@Test
	public void testGetAllKeys ()
	{
		final nvReadableTable nvrt = new nvReadableTable ( props );
		assertEquals( props.size() , nvrt.getAllKeys().size() );
	}

	@Test
	public void testGetCopyAsMap ()
	{
		final nvReadableTable nvrt = new nvReadableTable ( props );
		assertEquals( props.size() , nvrt.getCopyAsMap().size() );
	}
}
