package io.continual.util.console;

import org.junit.Test;

import io.continual.util.console.ConsoleProgram.Looper;
import io.continual.util.console.ConsoleProgram.StartupFailureException;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;
import io.continual.util.nv.impl.nvReadableTable;
import junit.framework.TestCase;

public class DaemonConsoleTest extends TestCase
{
	@Test
	public void testGetProgramName ()
	{
		final String expect = "batch";
		final DaemonConsole dc = new DaemonConsole ( expect );
		assertEquals( expect , dc.getProgramName() );
	}

	@Test
	public void testRegisterCopyrightHolder ()
	{
		final DaemonConsole dc = new DaemonConsole ( "batch" );
		dc.registerCopyrightHolder( "copyright" , 2022 );
		assertNotNull( dc.getCopyrightLines() );
	}

	@Test
	public void testGetCopyrightLines ()
	{
		final DaemonConsole dc = new DaemonConsole ( "batch" );
		dc.registerCopyrightHolder( "complete" , 2022 );
		assertEquals( 2 , dc.getCopyrightLines().size() );
	}

	@Test
	public void testInit_Quiet ()
	{
		final DaemonConsole dc = new DaemonConsole ( "batch" );
		dc.quietStartup();
		try {
			Looper loop = dc.init( new nvReadableTable() , new CmdLinePrefs( new CmdLineParser() ) );
			assertNotNull( loop );
		} catch (MissingReqdSettingException | InvalidSettingValueException | StartupFailureException e) {
			fail( "Expected init to be completed. " + e.getMessage() );
		}		
	}

	@Test
	public void testInit_NotQuiet ()
	{
		final DaemonConsole dc = new DaemonConsole ( "batch" );
		try {
			BackgroundLooper loop = (BackgroundLooper)dc.init( new nvReadableTable() , new CmdLinePrefs( new CmdLineParser() ) );
			assertNotNull( loop );
			assertFalse( loop.stillRunning() );
		} catch (MissingReqdSettingException | InvalidSettingValueException | StartupFailureException e) {
			fail( "Expected init to be completed. " + e.getMessage() );
		}		
	}

	@Test
	public void testInit_Shutdown ()
	{
		final DaemonConsole dc = new DaemonConsole ( "batch" );
		final nvReadableTable nvrt = new nvReadableTable(); 
		try {
			BackgroundLooper loop = (BackgroundLooper)dc.init( nvrt , new CmdLinePrefs( new CmdLineParser() ) );
			assertNotNull( loop );
			loop.teardown( nvrt );
		} catch (MissingReqdSettingException | InvalidSettingValueException | StartupFailureException e) {
			fail( "Expected init to be completed. " + e.getMessage() );
		}
	}
}
