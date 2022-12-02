package io.continual.util.nv.impl;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.TestCase;

public class nvWriteableTableTest extends TestCase
{
	@SuppressWarnings("serial")
	private final Map<String , String> hmKeyValue = new HashMap<String , String> () {{
		put( "timeStart" , "-604800000" );	put( "timeScale" , "900" );
	}};
	
	@Test
	public void testConstructorNoArgs ()
	{
		final nvWriteableTable nvwt = new nvWriteableTable ();
		assertEquals( 0 , nvwt.size() );
	}

	@Test
	public void testConstructorWithMapArgs ()
	{
		final nvWriteableTable nvwt = new nvWriteableTable ( hmKeyValue );
		assertEquals( hmKeyValue.size() , nvwt.size() );
	}

	@Test
	public void testConstructorWithReadableArgs ()
	{
		final nvReadableTable nvrt = new nvReadableTable ( hmKeyValue );
		final nvWriteableTable nvwt = new nvWriteableTable ( nvrt );
		assertEquals( hmKeyValue.size() , nvwt.size() );
	}

	@Test
	public void testConstructorWithReadableNullArgs ()
	{
		final nvReadableTable nvrt = null;
		final nvWriteableTable nvwt = new nvWriteableTable ( nvrt );
		assertEquals( 0 , nvwt.size() );
	}

	@Test
	public void testSetString ()
	{
		final nvWriteableTable nvwt = new nvWriteableTable ();
		nvwt.set( "timeStart" , "-604800000" );
		assertEquals( 1 , nvwt.size() );
	}

	@Test
	public void testSetChar ()
	{
		final char val = 'c';
		final nvWriteableTable nvwt = new nvWriteableTable ();
		nvwt.set( "key1" , val );
		assertEquals( 1 , nvwt.size() );
	}

	@Test
	public void testSetInt ()
	{
		final int val = 1;
		final nvWriteableTable nvwt = new nvWriteableTable ();
		nvwt.set( "key1" , val );
		assertEquals( 1 , nvwt.size() );
	}

	@Test
	public void testSetLong ()
	{
		final long val = 1;
		final nvWriteableTable nvwt = new nvWriteableTable ();
		nvwt.set( "key1" , val );
		assertEquals( 1 , nvwt.size() );
	}

	@Test
	public void testSetDouble ()
	{
		final double val = 1.0;
		final nvWriteableTable nvwt = new nvWriteableTable ();
		nvwt.set( "key1" , val );
		assertEquals( 1 , nvwt.size() );
	}

	@Test
	public void testSetBoolean ()
	{
		final boolean val = true;
		final nvWriteableTable nvwt = new nvWriteableTable ();
		nvwt.set( "key1" , val );
		assertEquals( 1 , nvwt.size() );
	}

	@Test
	public void testSetMap ()
	{
		final nvWriteableTable nvwt = new nvWriteableTable ();
		nvwt.set( hmKeyValue );
		assertEquals( hmKeyValue.size() , nvwt.size() );
	}

	@Test
	public void testSetByte ()
	{
		final byte[] val = "value".getBytes();
		final nvWriteableTable nvwt = new nvWriteableTable ();
		nvwt.set( "key1" , val );
		assertEquals( 1 , nvwt.size() );
	}

	@Test
	public void testSetByteOffLen ()
	{
		final byte[] val = "value".getBytes();
		final nvWriteableTable nvwt = new nvWriteableTable ();
		nvwt.set( "key1" , val , 0 , val.length );
		assertEquals( 1 , nvwt.size() );
	}

	@Test
	public void testSetStringArray ()
	{
		final String[] val = new String[] { "val1" , "val2" , "val3" };
		final nvWriteableTable nvwt = new nvWriteableTable ();
		nvwt.set( "key1" , val );
		assertEquals( 1 , nvwt.size() );
	}

	@Test
	public void testUnSet ()
	{
		final nvWriteableTable nvwt = new nvWriteableTable ();
		nvwt.set( "key1" , "value1" );
		nvwt.unset( "key1" );
		assertEquals( 0 , nvwt.size() );
	}
}
