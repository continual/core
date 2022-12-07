package io.continual.util.console;

import java.util.Properties;

import org.junit.Test;

import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;
import io.continual.util.nv.impl.nvReadableTable;
import io.continual.util.nv.impl.nvWriteableTable;
import junit.framework.TestCase;

public class ConfiguredConsoleTest extends TestCase
{
	@SuppressWarnings("serial")
	private final Properties props = new Properties () {{
		put( ConfiguredConsole.kConfigFile , "config.properties" );
	}};

	@Test
	public void testSetupDefaults ()
	{
		final TestConfiguredConsole tcc = new TestConfiguredConsole ();
		assertNotNull( tcc.setupDefaults( new nvWriteableTable() ) );
	}

	@Test
	public void testSetupOptions ()
	{
		final TestConfiguredConsole tcc = new TestConfiguredConsole ();
		assertNotNull( tcc.setupOptions( new CmdLineParser() ) );
	}

	@Test
	public void testLoadFile ()
	{
		final TestConfiguredConsole tcc = new TestConfiguredConsole ();
		try {
			assertNull( tcc.loadFile( "config.properties" ) );
		} catch (LoadException e) {
			fail( "Expected to run even if file does not exist. " + e.getMessage() );
		}
	}

	@Test
	public void testLoadAdditionalConfig1 ()
	{
		final nvReadableTable nvrt = new nvReadableTable ( props );
		final TestConfiguredConsole tcc = new TestConfiguredConsole ();
		try {
			assertNull( tcc.loadAdditionalConfig( nvrt ) );
		} catch (LoadException e) {
			fail( "Expected to run even if config properties does not exist. " + e.getMessage() );
		}
	}

	@Test
	public void testLoadAdditionalConfig ()
	{
		final nvReadableTable nvrt = new nvReadableTable ();
		final TestConfiguredConsole tcc = new TestConfiguredConsole ( "config" );
		try {
			assertNull( tcc.loadAdditionalConfig( nvrt ) );
		} catch (LoadException e) {
			fail( "Expected to run even if config properties does not exist. " + e.getMessage() );
		}
	}

	private class TestConfiguredConsole extends ConfiguredConsole
	{
		protected TestConfiguredConsole () 
		{
			super();
		}
		protected TestConfiguredConsole ( String shortName )
		{
			super( shortName );
		}
		@Override
		protected Looper init(NvReadable p, CmdLinePrefs clp)
				throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException 
		{
			return null;
		}		
	}
}
