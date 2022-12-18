package io.continual.iam.tools;

import java.io.PrintStream;
import java.util.Vector;

import org.junit.Assert;
import org.junit.Test;

import io.continual.iam.IamDb;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.util.console.ConsoleProgram.StartupFailureException;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class IamDbToolTest
{
	@Test
	public void testInit ()
	{
		final TestIamDbTool tidt = new TestIamDbTool ();
		try {
			Assert.assertNotNull ( tidt.init ( null , null ) );
		} catch (MissingReqdSettingException | InvalidSettingValueException | StartupFailureException e) {
			Assert.fail ( "Expected to execute but fails with exception " + e.getMessage () );
		}
	}

	private static class TestIamDbTool extends IamDbTool<Identity, Group>
	{
		public TestIamDbTool ()
		{
			super ();
		}
		@Override
		protected IamDb<Identity, Group> createDb(Vector<String> args, PrintStream outTo) throws IamSvcException {
			return null;
		}
	}
}
