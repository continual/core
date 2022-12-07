package io.continual.util.console;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import io.continual.util.console.ConsoleProgram.StartupFailureException;
import io.continual.util.console.ConsoleProgram.UsageException;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;
import junit.framework.TestCase;

public class ConsoleProgramTest extends TestCase
{
	@Test
	public void testGetCmdLineParser ()
	{
		final ConsoleProgram cp = new ConsoleProgram ();
		assertNotNull( cp.getCmdLineParser() );
	}

	@Test
	public void testExpandFileArg ()
	{
		final ConsoleProgram cp = new ConsoleProgram ();
		final List<File> result = cp.expandFileArg( Paths.get("").toAbsolutePath().toString() );
		assertNotNull( result );
	}

	@Test
	public void testRunFromMain ()
	{
		final ConsoleProgram cp = new ConsoleProgram ();
		final CmdLineParser clp = cp.getCmdLineParser();
		clp.registerOptionWithValue( "option" , "o" , null , null );
		try {
			cp.runFromMain( new String[] { "--option" , "0" } );
		} catch (UsageException | LoadException | MissingReqdSettingException | InvalidSettingValueException
				| StartupFailureException e) {
			fail( "Expected to run. " + e.getMessage() );
		}
	}

	@Test
	public void testStartupFailureException1 ()
	{
		final String expect = "Test StartupFailureException";
		try {
			throw new StartupFailureException( expect );
		} catch( StartupFailureException result ) {
			assertEquals( expect , result.getMessage() );
		}		
	}

	@Test
	public void testStartupFailureException2 ()
	{
		final String expect = "Test StartupFailureException";
		try {
			throw new StartupFailureException( new Exception( expect ) );
		} catch( StartupFailureException result ) {
			assertTrue( result.getMessage().contains( expect ) );
		}
	}

	@Test
	public void testStartupFailureException3 ()
	{
		final String expect = "Test StartupFailureException";
		try {
			throw new StartupFailureException( expect , new Exception( expect ) );
		} catch( StartupFailureException result ) {
			assertEquals( expect , result.getMessage() );
		}
	}

	@Test
	public void testUsageException1 ()
	{
		final String expect = "Test UsageException";
		try {
			throw new UsageException( expect );
		} catch( UsageException result ) {
			assertEquals( expect , result.getMessage() );
		}
	}

	@Test
	public void testUsageException2 ()
	{
		final String expect = "Test UsageException";
		try {
			throw new UsageException( new Exception ( expect ) );
		} catch( UsageException result ) {
			assertTrue( result.getMessage().contains( expect ) );
		}
	}
}
