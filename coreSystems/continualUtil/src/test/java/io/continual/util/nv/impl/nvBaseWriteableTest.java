package io.continual.util.nv.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.TestCase;

public class nvBaseWriteableTest extends TestCase
{
	@Test
	public void testSetChar ()
	{
		TestNvBaseWriteable tnvbw = new TestNvBaseWriteable ();
		tnvbw.set( "key" , 'c' );
		assertNotNull( tnvbw );
	}

	@Test
	public void testSetBoolean ()
	{
		TestNvBaseWriteable tnvbw = new TestNvBaseWriteable ();
		tnvbw.set( "key" , true );
		assertNotNull( tnvbw );
	}

	@Test
	public void testSetInt ()
	{
		TestNvBaseWriteable tnvbw = new TestNvBaseWriteable ();
		tnvbw.set( "key" , 1 );
		assertNotNull( tnvbw );
	}

	@Test
	public void testSetLong ()
	{
		TestNvBaseWriteable tnvbw = new TestNvBaseWriteable ();
		tnvbw.set( "key" , 1L );
		assertNotNull( tnvbw );
	}

	@Test
	public void testSetDouble ()
	{
		TestNvBaseWriteable tnvbw = new TestNvBaseWriteable ();
		tnvbw.set( "key" , 1.0 );
		assertNotNull( tnvbw );
	}

	@Test
	public void testSetByte1 ()
	{
		final byte[] expect = "value".getBytes();
		TestNvBaseWriteable tnvbw = new TestNvBaseWriteable ();
		tnvbw.set( "key" , expect );
		assertNotNull( tnvbw );
	}

	@Test
	public void testSetByte2 ()
	{
		final byte[] expect = "value".getBytes();
		TestNvBaseWriteable tnvbw = new TestNvBaseWriteable ();
		tnvbw.set( "key" , expect , 0, expect.length );
		assertNotNull( tnvbw );
	}

	@Test
	public void testSetStringArray ()
	{
		TestNvBaseWriteable tnvbw = new TestNvBaseWriteable ();
		tnvbw.set( "key" , new String[] { "value1" , "value2" } );
		assertNotNull( tnvbw );
	}

	@Test
	public void testSetMap ()
	{
		@SuppressWarnings("serial")
		final Map<String, String> hmKeyValue = new HashMap<String, String> () {{
			put( "key1" , "value1" );	put( "key2" , "value2" );	}};
		TestNvBaseWriteable tnvbw = new TestNvBaseWriteable ();
		tnvbw.set( hmKeyValue );
		assertNotNull( tnvbw );
	}

	private class TestNvBaseWriteable extends nvBaseWriteable
	{
		@Override
		public void clear() {	}
		@Override
		public void unset(String key) {	}
		@Override
		public void set(String key, String value) {	}
		@Override
		public int size() {	return 0;	}
		@Override
		public Collection<String> getAllKeys() {	return null;	}
		@Override
		public Map<String, String> getCopyAsMap() {	return null;	}
		@Override
		public boolean hasValueFor(String key) {	return false;	}
		@Override
		public String getString(String key) throws MissingReqdSettingException {
			return null;
		}
		@Override
		public String[] getStrings(String key) throws MissingReqdSettingException {
			return null;
		}
	}
}
