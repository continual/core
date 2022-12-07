package io.continual.util.console;

import org.junit.Test;

import io.continual.util.nv.NvReadable.MissingReqdSettingException;
import junit.framework.TestCase;

public class CmdLinePrefsTest extends TestCase
{
	private CmdLineParser parser;

	@Override
	protected void setUp()
	{
		parser = new CmdLineParser ();
		parser.registerOnOffOption( "verbose" , 'v' , true );
	}

	@Test
	public void testAddLeftover ()
	{
		final int expect = 1;
		final CmdLinePrefs pref = new CmdLinePrefs ( parser );
		pref.addLeftover( "leftover" );
		assertEquals( expect , pref.getFileArguments().size() );
	}

	@Test
	public void testGetFileArguments ()
	{
		final CmdLinePrefs pref = new CmdLinePrefs ( parser );
		assertEquals( 0 , pref.getFileArguments().size() );
	}

	@Test
	public void testGetFileArgumentsAsString ()
	{
		final String expect = "leftover";
		final CmdLinePrefs pref = new CmdLinePrefs ( parser );
		pref.addLeftover( expect );
		assertEquals( expect , pref.getFileArgumentsAsString() );
	}

	@Test
	public void testWasExplicitlySet ()
	{
		final CmdLinePrefs pref = new CmdLinePrefs ( parser );
		pref.set( "key" , "value" );
		assertTrue( pref.hasValueFor( "key" ) );
	}

	@Test
	public void testGetString_WasExplicitlySet ()
	{
		final CmdLinePrefs pref = new CmdLinePrefs ( parser );
		pref.set( "key" , "value" );
		try {
			assertEquals( "value" , pref.getString( "key" ) );
		} catch (MissingReqdSettingException e) {
			fail( "Expected to get string for key. " + e.getMessage());
		}
	}

	@Test
	public void testGetString_Parser ()
	{
		final CmdLinePrefs pref = new CmdLinePrefs ( parser );
		try {
			pref.getString( "verbose" );
		} catch (MissingReqdSettingException e) {
			fail( "Expected to get string for key. " + e.getMessage());
		}
	}
}
