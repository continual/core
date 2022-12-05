package io.continual.util.nv.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.junit.Test;

import io.continual.util.nv.NvReadable.LoadException;
import junit.framework.TestCase;

public class nvBaseReadableTest extends TestCase
{
	@Test
	public void testToString ()
	{
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , "value1" );
		tnvbr.getDataReference().put( "key2" , "value2" );
		assertNotNull( tnvbr.toString() );
	}

	@Test
	public void testGet ()
	{
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , "value1" );
		assertNotNull( tnvbr.get( "key1" ) );
	}

	@Test
	public void testGetBoolean ()
	{
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , true );
		assertTrue( tnvbr.getBoolean( "key1" , true ) );
	}

	@Test
	public void testGetBooleanDefault ()
	{
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , true );
		assertFalse( tnvbr.getBoolean( "key2" , false ) );
	}

	@Test
	public void testGetInt ()
	{
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , 1 );
		assertEquals( 1 , tnvbr.getInt( "key1" , 1 ) );
	}

	@Test
	public void testGetIntDefault ()
	{
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , 1 );
		assertEquals( 1 , tnvbr.getInt( "key2" , 1 ) );
	}

	@Test
	public void testGetDouble ()
	{
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , 1.0 );
		assertEquals( 1.0 , tnvbr.getDouble( "key1" , 1.0 ) );
	}

	@Test
	public void testGetDoubleDefault ()
	{
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , 1.0 );
		assertEquals( 1.0 , tnvbr.getDouble( "key2" , 1.0 ) );
	}

	@Test
	public void testGetStrings ()
	{
		final String[] arrValue = new String[] { "value1" , "value2" };
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , arrValue );
		assertEquals( arrValue , tnvbr.getStrings( "key1" , arrValue ) );
	}

	@Test
	public void testGetCharacter ()
	{
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , "c" );
		assertEquals( 'c' , tnvbr.getCharacter( "key1" , 'c' ) );
	}

	@Test
	public void testGetCharacterDefault ()
	{
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , "c" );
		assertEquals( 'c' , tnvbr.getCharacter( "key2" , 'c' ) );
	}

	@Test
	public void testGetLong ()
	{
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , 1L );
		assertEquals( 1L , tnvbr.getLong( "key1" , 1L ) );
	}

	@Test
	public void testGetLongDefault ()
	{
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , 1L );
		assertEquals( 1L , tnvbr.getLong( "key2" , 1L ) );
	}

	@Test
	public void testGetBytes ()
	{
		final byte[] arrBytes = "value".getBytes();
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , arrBytes );
		assertNotNull( tnvbr.getBytes( "key1" , arrBytes ) );
	}

	@Test
	public void testCopyIntoWritable ()
	{
		nvWriteableTable nvwt = new nvWriteableTable ();
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , "value1" );
		tnvbr.copyInto( nvwt );
		assertEquals( 1 , nvwt.size() );
	}

	@Test
	public void testCopyInto ()
	{
		Map<String, String> hmKeyValue = new HashMap<> ();
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		tnvbr.getDataReference().put( "key1" , "value1" );
		tnvbr.copyInto( hmKeyValue );
		assertEquals( tnvbr.getDataReference().size() , hmKeyValue.size() );
	}

	@Test
	public void testEval ()
	{
		TestNvBaseReadable tnvbr = new TestNvBaseReadable ();
		try {
			tnvbr.rescan();
		} catch (LoadException e) {	}
		assertNotNull( tnvbr.eval( "test" ) );
	}

	private class TestNvBaseReadable extends nvBaseReadable
	{
		private Map<String, Object> fData;

		public TestNvBaseReadable () {
			super();
			fData = new HashMap<String, Object> ();
		}
		@Override
		public int size() {	return fData.size();	}
		@Override
		public Collection<String> getAllKeys() {	
			final TreeSet<String> list = new TreeSet<String> ();
			for ( Object o : fData.keySet () ) {
				list.add ( o.toString () );
			}
			return list;
		}
		@Override
		public Map<String, String> getCopyAsMap() {	
			HashMap<String,String> map = new HashMap<String,String> ();
			for ( Entry<String, Object> e : fData.entrySet () )
			{
				map.put ( e.getKey(), (String)e.getValue() );
			}
			return map;
		}
		@Override
		public boolean hasValueFor(String key) {	return false;	}
		@Override
		public String getString(String key) throws MissingReqdSettingException {
			try {
				return String.valueOf(fData.get(key));
			} catch( Exception e) {
				throw new MissingReqdSettingException( key, e);
			}
		}
		@Override
		public String[] getStrings(String key) throws MissingReqdSettingException {
			return (String[])fData.get( key );
		}
		public Map<String, Object> getDataReference () {
			return fData;
		}
	}
}
