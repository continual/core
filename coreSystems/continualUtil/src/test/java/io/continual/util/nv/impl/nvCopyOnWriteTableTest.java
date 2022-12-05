package io.continual.util.nv.impl;

import java.util.Map;
import java.util.HashMap;

import org.junit.Test;

import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;
import junit.framework.TestCase;

public class nvCopyOnWriteTableTest extends TestCase 
{
	@SuppressWarnings("serial")
	private final Map<String, String> hmKeyValue = new HashMap<String, String> () {{
		put( "key1" , "value1" );	put( "key2" , "value2" );	}};

	@Test
	public void testConstructorNoArgs ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ();
		assertEquals( 0 , nvcowt.size() );
	}

	@Test
	public void testConstructorWithArgs1 ()
	{
		nvCopyOnWriteTable nvcowt1 = new nvCopyOnWriteTable ();
		nvcowt1.set( "key1" , "value1" );
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( nvcowt1 );
		assertEquals( 1 , nvcowt.size() );
	}

	@Test
	public void testConstructorWithArgs2 ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		assertEquals( 2 , nvcowt.size() );
	}

	@Test
	public void testHasValueFor ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		assertTrue( nvcowt.hasValueFor( "key1" ) );
	}

	@Test
	public void testGetString ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		try {
			assertEquals( hmKeyValue.get( "key1" ) , nvcowt.getString( "key1" ) );
		} catch (MissingReqdSettingException e) {
			fail( "Expected to get value for key." );
		}
	}

	@Test
	public void testGetStrings ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ();
		nvcowt.set( "key1" , new String[] { "value1" , "value2" } );
		try {
			assertEquals( 2 , nvcowt.getStrings( "key1" ).length );
		} catch (MissingReqdSettingException e) {
			fail( "Expected to get value for key." );
		}
	}

	@Test
	public void testCopyAsMap ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		assertEquals( hmKeyValue.size() , nvcowt.getCopyAsMap().size() );
	}

	@Test
	public void testGetAllKeys ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		assertEquals( hmKeyValue.size() , nvcowt.getAllKeys().size() );		
	}

	@Test
	public void testCopyInto ()
	{
		nvWriteableTable nvwt = new nvWriteableTable ( hmKeyValue );
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		nvcowt.copyInto( nvwt );
		assertEquals( hmKeyValue.size() , nvcowt.getAllKeys().size() );
	}

	@Test
	public void testCopyIntoMap ()
	{
		final Map<String , String> mapKV = new HashMap<String, String> ();
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		nvcowt.copyInto( mapKV );
		assertEquals( nvcowt.size() , mapKV.size() );
	}

	@Test
	public void testGetDataReference ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		assertEquals( hmKeyValue.size() , nvcowt.getDataReference().size() );
	}

	@Test
	public void testClear ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		nvcowt.clear();
		assertEquals( 0 , nvcowt.size() );
	}

	@Test
	public void testSetMap ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ();
		nvcowt.set( hmKeyValue );
		assertEquals( hmKeyValue.size() , nvcowt.size() );
	}

	@Test
	public void testSetInt ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ();
		nvcowt.set( "key" , 1 );
		try {
			assertEquals( 1 , nvcowt.getInt( "key" ) );
		} catch (MissingReqdSettingException e) {
			fail( "Expected value for given key." );
		}
	}

	@Test
	public void testSetLong ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ();
		nvcowt.set( "key" , 1L );
		try {
			assertEquals( 1L , nvcowt.getLong( "key" ) );
		} catch (MissingReqdSettingException e) {
			fail( "Expected value for given key." );
		}
	}

	@Test
	public void testSetDouble ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ();
		nvcowt.set( "key" , 1.0 );
		try {
			assertEquals( 1.0 , nvcowt.getDouble( "key" ) );
		} catch (MissingReqdSettingException e) {
			fail( "Expected value for given key." );
		}
	}

	@Test
	public void testSetBoolean ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ();
		nvcowt.set( "key" , true );
		try {
			assertTrue( nvcowt.getBoolean( "key" ) );
		} catch (MissingReqdSettingException e) {
			fail( "Expected value for given key." );
		}
	}

	@Test
	public void testSetChar ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ();
		nvcowt.set( "key" , 'c' );
		try {
			assertEquals( 'c' , nvcowt.getCharacter( "key" ) );
		} catch (MissingReqdSettingException e) {
			fail( "Expected value for given key." );
		}
	}

	@Test
	public void testSetByte1 ()
	{
		final byte[] arrByte = "value".getBytes();
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ();
		nvcowt.set( "key" , arrByte );
		try {
			assertNotNull( nvcowt.getBytes( "key" ) );
		} catch (MissingReqdSettingException e) {
			fail( "Expected value for given key." );
		} catch (InvalidSettingValueException e) {
			fail( "Expected value for given key." );
		}
	}

	@Test
	public void testSetByte2 ()
	{
		final byte[] arrByte = "value".getBytes();
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ();
		nvcowt.set( "key" , arrByte , 0 , arrByte.length );
		try {
			assertNotNull( nvcowt.getBytes( "key" ) );
		} catch (MissingReqdSettingException e) {
			fail( "Expected value for given key." );
		} catch (InvalidSettingValueException e) {
			fail( "Expected value for given key." );
		}
	}

	@Test
	public void testSetNull ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ();
		nvcowt.set( "key" , (String)null );
		assertEquals( 0 , nvcowt.size() );
	}

	@Test
	public void testUnset ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		nvcowt.unset( "key1" );
		assertEquals( ( hmKeyValue.size() - 1 ) , nvcowt.size() ); 
	}

	// data inner class
	@Test
	public void testDataRef ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		assertEquals( 1 , nvcowt.getDataReference().getRefCount() );
	}

	@Test
	public void testDataPrepForWrite ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		nvcowt.getDataReference().attach();
		nvcowt.getDataReference().attach();
		nvcowt.clear();
		assertEquals( 0 , nvcowt.size() );
	}

	@Test
	public void testDataEstimateWeight ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		assertTrue( nvcowt.getDataReference().estimateWeight() >= 0.0 );
	}

	@Test
	public void testDataEntrySet ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		assertEquals( hmKeyValue.size() , nvcowt.getDataReference().entrySet().size() );
	}

	@Test
	public void testDataCalcWeight ()
	{
		nvCopyOnWriteTable nvcowt = new nvCopyOnWriteTable ( hmKeyValue );
		assertTrue( nvcowt.getDataReference().calcWeight( "key" , null ) > 0L );
	}
}
