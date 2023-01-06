package io.continual.services;

import java.util.Map;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import io.continual.util.console.CmdLineParser;
import io.continual.util.console.ConsoleProgram.StartupFailureException;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;
import io.continual.util.nv.impl.nvReadableTable;
import io.continual.util.nv.impl.nvWriteableTable;
import junit.framework.TestCase;

public class ServerTest extends TestCase
{
	@Test
	public void testSetupDefaults ()
	{
		final Server<ServiceContainer> server = new Server<> ( "test" , new Server.StdFactory () );
		assertNotNull ( server );
		assertNotNull ( server.setupDefaults ( new nvWriteableTable () ) );
	}

	@Test
	public void testSetupOptions ()
	{
		final Server<ServiceContainer> server = new Server<> ( "test" , new Server.StdFactory () );
		assertNotNull ( server.setupOptions ( new CmdLineParser () ) );
	}

	@Test
	public void testInit ()
	{
		final Map<String, String> keyVal = new HashMap<> ();
		keyVal.put ( Server.kServices , "invalid.json" );

		final Server<ServiceContainer> server = new Server<> ( "test" , new Server.StdFactory () );
		try {
			// Invalid
			assertNotNull ( server.init ( new nvReadableTable ( keyVal ) , null ) );
			// No Services
			assertNotNull ( server.init ( new nvReadableTable () , null ) );
			// Valid
			keyVal.put ( Server.kServices , "services.json" );
			assertNotNull ( server.init ( new nvReadableTable ( keyVal ) , null ) );
		} catch (MissingReqdSettingException | InvalidSettingValueException | StartupFailureException e) {
			Assert.fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}

	@Test
	public void testServices ()
	{
		final Map<String, String> keyVal = new HashMap<> ();
		keyVal.put ( Server.kServices , "services.json" );

		final Server<ServiceContainer> server = new Server<> ( "test" , new Server.StdFactory () );
		try {
			server.hideCopyrights ();
			server.init ( new nvReadableTable ( keyVal ) , null );
			assertNotNull ( server.getServices() );
			assertTrue ( server.daemonStillRunning () );
			server.shutdown ();
			assertFalse ( server.daemonStillRunning () );
		} catch (MissingReqdSettingException | InvalidSettingValueException | StartupFailureException | InterruptedException e) {
			Assert.fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}

	@Test
	public void testRunServer ()
	{
		try {
			Server.runServer( "test" , null );
		} catch (Exception e) {
			// Added for coverage
		}
	}
}
