package io.continual.util.nv.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Properties;

import org.junit.Test;

import junit.framework.TestCase;

import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class nvPropertiesFileTest extends TestCase
{
	@SuppressWarnings("serial")
	private final static Properties props = new Properties () {{
		put( "timeStart" , "-604800000" );	put( "timeScale" , "900" );
	}};
	
	@Test
	public void testConstructorFileArgs ()
	{
		final File file = new File( nvPropertiesFileTest.class.getResource( "nvPropertiesFileTest.class" ).getPath() );
		try {
			final nvPropertiesFile nvpf = new nvPropertiesFile ( file );
			assertNotNull( nvpf );
		} catch ( LoadException le ) {
			fail( "Expected file to get loaded." );
		}
	}

	@Test
	public void testConstructorStreamArgs ()
	{
		final File file = new File( nvPropertiesFileTest.class.getResource( "nvPropertiesFileTest.class" ).getPath() );
		try {
			final nvPropertiesFile nvpf = new nvPropertiesFile ( new FileInputStream ( file ) );
			assertNotNull( nvpf );
		} catch ( LoadException le ) {
			fail( "Expected file to get loaded. " + le.getMessage() );
		} catch (FileNotFoundException fe) {
			fail( "Expected file to get loaded. " + fe.getMessage() );
		}
	}

	@Test
	public void testConstructorURLArgs ()
	{
		final File file = new File( nvPropertiesFileTest.class.getResource( "nvPropertiesFileTest.class" ).getPath() );
		try {
			final nvPropertiesFile nvpf = new nvPropertiesFile ( file.toURI().toURL() );
			assertNotNull( nvpf );
		} catch ( LoadException le ) {
			fail( "Expected file to get loaded. " + le.getMessage() );
		} catch (MalformedURLException murle) {
			fail( "Expected file to get loaded. " + murle.getMessage() );
		}
	}

	@Test
	public void testConstructorStreamFetcherArgs ()
	{
		try {
			final nvPropertiesFile nvpf = new nvPropertiesFile ( new TestStreamFetcher () );
			assertNotNull( nvpf );
		} catch ( LoadException le ) {
			fail( "Expected file to get loaded. " + le.getMessage() );
		}
	}

	@Test
	public void testGetString ()
	{
		try {
			final nvPropertiesFile nvpf = new nvPropertiesFile ( new TestStreamFetcher () );
			assertEquals( props.get( "timeStart" ) , nvpf.getString( "timeStart" ) );
		} catch ( LoadException le ) {
			fail( "Expected file to get loaded. " + le.getMessage() );
		} catch (MissingReqdSettingException mrse) {
			fail( "Expected ro read property. " + mrse.getMessage() );
		}		
	}

	@Test
	public void testGetStrings ()
	{
		try {
			final nvPropertiesFile nvpf = new nvPropertiesFile ( new TestStreamFetcher () );
			assertNotNull( nvpf.getStrings( "timeStart" ) );
		} catch ( LoadException le ) {
			fail( "Expected file to get loaded. " + le.getMessage() );
		} catch (MissingReqdSettingException mrse) {
			fail( "Expected ro read property. " + mrse.getMessage() );
		}		
	}

	@Test
	public void testHasValueFor ()
	{
		try {
			final nvPropertiesFile nvpf = new nvPropertiesFile ( new TestStreamFetcher () );
			assertTrue( nvpf.hasValueFor( "timeStart" ) );
		} catch ( LoadException le ) {
			fail( "Expected file to get loaded. " + le.getMessage() );
		}		
	}

	@Test
	public void testSize ()
	{
		try {
			final nvPropertiesFile nvpf = new nvPropertiesFile ( new TestStreamFetcher () );
			assertEquals( props.size() , nvpf.size() );
		} catch ( LoadException le ) {
			fail( "Expected file to get loaded. " + le.getMessage() );
		}		
	}

	@Test
	public void testGetAllKeys ()
	{
		try {
			final nvPropertiesFile nvpf = new nvPropertiesFile ( new TestStreamFetcher () );
			assertEquals( props.size() , nvpf.getAllKeys().size() );
		} catch ( LoadException le ) {
			fail( "Expected file to get loaded. " + le.getMessage() );
		}		
	}

	@Test
	public void testCopyAsMap ()
	{
		try {
			final nvPropertiesFile nvpf = new nvPropertiesFile ( new TestStreamFetcher () );
			assertEquals( props.size() , nvpf.getCopyAsMap().size() );
		} catch ( LoadException le ) {
			fail( "Expected file to get loaded. " + le.getMessage() );
		}		
	}

	private static class TestStreamFetcher implements nvPropertiesFile.StreamFetcher
	{		
		@Override
		public InputStream getStream() throws LoadException
		{
			try {
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				props.store(output, null);
				return new ByteArrayInputStream(output.toByteArray());
			} catch ( IOException e ) {
				throw new LoadException ( e );
			}
		}		
	}
}
