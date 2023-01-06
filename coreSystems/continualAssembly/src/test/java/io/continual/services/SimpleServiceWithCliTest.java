package io.continual.services;

import java.io.PrintStream;
import java.util.HashMap;

import org.junit.Test;

import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConsoleProgram.UsageException;
import io.continual.util.console.shell.ConsoleLooper.InputResult;
import io.continual.util.console.shell.SimpleCommand;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;
import junit.framework.TestCase;

public class SimpleServiceWithCliTest extends TestCase
{
	@Test
	public void testConstructor ()
	{
		assertNotNull ( new TestSimpleServiceWithCli () );
	}

	@Test
	public void testRegister ()
	{
		final SimpleServiceWithCli sswc = new TestSimpleServiceWithCli ();
		assertNotNull ( sswc.register ( new TestSimpleCommand ( "run" ) ) );
	}

	@Test
	public void testGetCommandFor ()
	{
		final SimpleServiceWithCli sswc = new TestSimpleServiceWithCli ();
		assertNull ( sswc.getCommandFor ( "run" ) );
		sswc.register ( new TestSimpleCommand ( "run" ) );
		try {
			assertNotNull ( sswc.getCommandFor ( "help" ).execute ( null , System.out ) );
			assertNotNull ( sswc.getCommandFor ( "help" ) );		
			sswc.register ( new TestSimpleCommand ( "help" ) );
			assertNotNull ( sswc.getCommandFor ( "help" ) );
		} catch (UsageException e) {
			fail ( "Expected to execute but failed with exception " + e.getMessage() );
		}
	}

	// Dummy Implementation to test abstract class
	private static class TestSimpleServiceWithCli extends SimpleServiceWithCli
	{
		protected TestSimpleServiceWithCli ()
		{
			super ();
		}		
	}

	private static class TestSimpleCommand extends SimpleCommand
	{
		protected TestSimpleCommand(String cmd)
		{
			super(cmd);
		}

		@Override
		protected InputResult execute(HashMap<String, Object> workspace, CmdLinePrefs p, PrintStream outTo)
				throws UsageException, MissingReqdSettingException
		{
			return null;
		}		
	}
}
