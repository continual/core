package io.continual.util.nv.impl;

import org.json.JSONObject;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.json.JSONArray;
import org.junit.Test;

import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;
import junit.framework.TestCase;

public class nvJsonObjectTest extends TestCase
{
	private final JSONObject jsonItemArray = new JSONObject () {{
		put( "arrkey1" , "arrvalue1" );		put( "arrkey2" , "arrvalue2" );
	}};
	private final JSONObject jsonObj = new JSONObject () {{
		put( "key1" , "value1");	put( "key2" , new JSONArray().put( jsonItemArray ) );
	}};

	@Test
	public void testConstructor ()
	{
		final nvJsonObject nvjo = new nvJsonObject ();
		assertNotNull( nvjo );
	}

	@Test
	public void testConstructorWithArgs ()
	{
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		assertNotNull( nvjo );
	}

	@Test
	public void testToJson ()
	{
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		assertNotNull( nvjo.toJson() );
	}

	@Test
	public void testGet ()
	{
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		assertNotNull( nvjo.get( "key1" ) );
		assertNull( nvjo.get( "key9" ) );
	}

	@Test
	public void testGetString ()
	{
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		try {
			assertNotNull( nvjo.getString( "key1" ) );
		} catch ( MissingReqdSettingException mrse ) {
			fail( "Expected to get key value." );
		}
	}

	@Test
	public void testSetCharacter ()
	{
		final nvJsonObject nvjo = new nvJsonObject ();
		nvjo.set( "key" , 'c');
		try {
			assertEquals( 'c' , nvjo.getCharacter( "key" ) );
		} catch ( MissingReqdSettingException mrse ) {
			fail( "Expected to get key value." );
		}
	}

	@Test
	public void testGetCharacterDefVal ()
	{
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		assertEquals( '9' , nvjo.getCharacter( "key9" , '9' ) );
		nvjo.set( "key9" , '9' );
		assertEquals( '9' , nvjo.getCharacter( "key9" , '9' ) );
	}

	@Test
	public void testSetInt ()
	{
		final nvJsonObject nvjo = new nvJsonObject ();
		nvjo.set( "key" , 1 );
		try {
			assertEquals( 1 , nvjo.getInt( "key" ) );
		} catch ( MissingReqdSettingException mrse ) {
			fail( "Expected to get key value." );
		}
	}

	@Test
	public void testGetIntDefVal ()
	{
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		assertEquals( 9 , nvjo.getInt( "key9" , 9 ) );
	}

	@Test
	public void testSetLong ()
	{
		final nvJsonObject nvjo = new nvJsonObject ();
		nvjo.set( "key" , 1L );
		try {
			assertEquals( 1L , nvjo.getLong( "key" ) );
		} catch ( MissingReqdSettingException mrse ) {
			fail( "Expected to get key value." );
		}
	}

	@Test
	public void testGetLongDefVal ()
	{
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		assertEquals( 9L , nvjo.getLong( "key9" , 9L ) );
	}

	@Test
	public void testSetDouble ()
	{
		final nvJsonObject nvjo = new nvJsonObject ();
		nvjo.set( "key" , 1.0 );
		try {
			assertEquals( 1.0 , nvjo.getDouble( "key" ) );
		} catch ( MissingReqdSettingException mrse ) {
			fail( "Expected to get key value." );
		}
	}

	@Test
	public void testGetDoubleDefVal ()
	{
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		assertEquals( 9.0 , nvjo.getDouble( "key9" , 9.0 ) );
	}

	@Test
	public void testSetBoolean ()
	{
		final nvJsonObject nvjo = new nvJsonObject ();
		nvjo.set( "key" , true );
		try {
			assertTrue( nvjo.getBoolean( "key" ) );
		} catch ( MissingReqdSettingException mrse ) {
			fail( "Expected to get key value." );
		}
	}

	@Test
	public void testGetBooleanDefVal ()
	{
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		nvjo.set( "bookey" , true );
		assertTrue( nvjo.getBoolean( "bookey" , true ) );
		assertTrue( nvjo.getBoolean( "key9" , true ) );
	}

	@Test
	public void testSetByts ()
	{
		final byte[] bytes = "value".getBytes();
		final nvJsonObject nvjo = new nvJsonObject ();
		nvjo.set( "key" , bytes );
		try {
			assertNotNull( nvjo.getBytes( "key" ) );
		} catch ( MissingReqdSettingException mrse ) {
			fail( "Expected to get key value." );
		} catch (InvalidSettingValueException e) {
			fail( "Expected to get key value." );
		}
	}

	@Test
	public void testGetBytesDefVal ()
	{
		final byte[] bytes = "value".getBytes();
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		nvjo.set( "bytekey" , bytes );
		assertNotNull( nvjo.getBytes( "bytekey" , bytes ) );
		assertNotNull( nvjo.getBytes( "key9" , bytes ) );
	}

	@Test
	public void testGetStrings ()
	{
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		try {
			assertNotNull( nvjo.getStrings( "key2" ) );
		} catch (MissingReqdSettingException e) {
			fail( "Expected to get key value." );
		}
	}

	@Test
	public void testGetStringsDefVal ()
	{
		final String[] defValues = new String[] { "val1" , "val2" };
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		assertNotNull( nvjo.getStrings( "key9" , defValues ) );
		assertNotNull( nvjo.getStrings( "key2" , defValues ) );
	}

	@Test
	public void testSize ()
	{
		final nvJsonObject nvjo = new nvJsonObject ();
		nvjo.set( "key1" , "value1" );
		nvjo.toString();
		assertEquals( 1 ,  nvjo.size() );
	}

	@Test
	public void testHasValueFor ()
	{
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		assertTrue( nvjo.hasValueFor( "key1" ) );
	}

	@Test
	public void testGetCopyAsMap ()
	{
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		assertTrue( nvjo.getCopyAsMap().size() > 1 );
	}

	@Test
	public void testSetStringArray ()
	{
		final String[] arrValues = new String[] { "val1" , "val2" };
		final nvJsonObject nvjo = new nvJsonObject ();
		nvjo.set( "key" , arrValues );
		assertEquals( 1 , nvjo.size() );
	}

	@Test
	public void testSetStringList ()
	{
		@SuppressWarnings("serial")
		final List<String> lstValues = new ArrayList<String>() {{ 
			add( "val1" );	add( "val2" ); 	}};
		final nvJsonObject nvjo = new nvJsonObject ();
		nvjo.set( "key" , lstValues );
		assertEquals( 1 , nvjo.size() );
	}

	@Test
	public void testSetStringMap ()
	{
		@SuppressWarnings("serial")
		final Map<String, String> keyValues = new HashMap<String, String>() {{ 
			put( "key1" , "val1" );		put( "key2" , "val2" ); 	}};
		final nvJsonObject nvjo = new nvJsonObject ();
		nvjo.set( keyValues );
		assertEquals( keyValues.size() , nvjo.size() );
	}

	@Test
	public void testUnSet ()
	{
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		nvjo.unset( "key1" );
		assertEquals( 1 , nvjo.size() );
	}

	@Test
	public void testClear ()
	{
		final nvJsonObject nvjo = new nvJsonObject ();
		nvjo.set( "key" , "value" );
		nvjo.clear();
		nvjo.rescan();
		assertNotNull( nvjo );
	}

	@Test
	public void testCopyInto ()
	{
		final nvJsonObject nvjo = new nvJsonObject ( jsonObj );
		nvjo.set( "intkey" , 1 );		nvjo.set( "longkey" , 1L );
		nvjo.set( "doublekey" , 1.0 );	nvjo.set( "booleankey" , true );
		final nvJsonObject nvjoCopy = new nvJsonObject ();
		nvjo.copyInto( nvjoCopy );
		assertTrue( nvjoCopy.size() > 0 );
	}

	@Test
	public void testGetAllKeys ()
	{
		final JSONObject json = new JSONObject ();
		json.put( "key1" , "val1" );	json.put( "key2" , "val2" );
		json.put( "key3" , new JSONObject() {{	put( "ikey1" , "ival1" ); }} );
		final nvJsonObject nvjo = new nvJsonObject ( json );
		assertTrue( nvjo.getAllKeys().size() > 0 );
	}
}
