package io.continual.util.nv.impl;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;

import org.junit.Test;

import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class ExceptionTest
{
	@Test(expected = NvReadable.MissingReqdSettingException.class)
	public void testNvReadableTable_InvalidKeyGetString () throws MissingReqdSettingException
	{
		final nvReadableTable nvrt = new nvReadableTable ();
		nvrt.getString( "key1" );
	}

	@Test(expected = NvReadable.MissingReqdSettingException.class)
	public void testNvReadableStack_InvalidKeyGetString () throws MissingReqdSettingException
	{
		final nvReadableTable nvrt = new nvReadableTable ();
		final nvReadableStack nvrs = new nvReadableStack ();
		nvrs.push( nvrt );
		nvrs.getString( "key1" );
	}

	@Test(expected = NvReadable.MissingReqdSettingException.class)
	public void testNvReadableStack_InvalidKeyGetStrings () throws MissingReqdSettingException
	{
		final nvReadableTable nvrt = new nvReadableTable ();
		final nvReadableStack nvrs = new nvReadableStack ();
		nvrs.push( nvrt );
		nvrs.getStrings( "key1" );
	}

	@Test(expected = NvReadable.MissingReqdSettingException.class)
	public void testNvPropertiesFile_InvalidKeyGetString () throws MissingReqdSettingException
	{
		final File file = new File( nvPropertiesFileTest.class.getResource( "ExceptionTest.class" ).getPath() );
		try {
			final nvPropertiesFile nvpf = new nvPropertiesFile ( file );
			nvpf.getString( "key1" );
		} catch ( LoadException le ) {
			fail( "Expected file to get loaded." );
		}
	}

	@Test(expected = NvReadable.LoadException.class)
	public void testNvPropertiesFile_InvalidFileConstructor () throws LoadException
	{
		final File file = new File( "1234567890.txt" );
		final nvPropertiesFile nvpf = new nvPropertiesFile ( file );
		nvpf.rescan();
	}

	@Test(expected = NvReadable.LoadException.class)
	public void testNvPropertiesFile_InvalidStreamRead () throws LoadException
	{
		final FileInputStream fis = null;
		final nvPropertiesFile nvpf = new nvPropertiesFile ( fis );
		nvpf.rescan();
	}

	@Test(expected = NvReadable.MissingReqdSettingException.class)
	public void testNvJsonObject_InvalidGetChar () throws MissingReqdSettingException
	{
		final nvJsonObject nvjo = new nvJsonObject ();
		nvjo.set( "key" , "value1" );
		nvjo.getCharacter( "key" );
	}
}
