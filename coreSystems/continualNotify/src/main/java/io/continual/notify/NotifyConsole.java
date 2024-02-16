
package io.continual.notify;

import java.io.PrintStream;
import java.util.HashMap;

import org.json.JSONObject;

import io.continual.notify.ContinualAlertAgent.Alert;
import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConsoleProgram;
import io.continual.util.console.shell.ConsoleLooper;
import io.continual.util.console.shell.SimpleCommand;
import io.continual.util.console.shell.StdCommandList;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class NotifyConsole extends ConsoleProgram
{
	public NotifyConsole ()
	{
		fNotifier = new ContinualNotifier ()
			.inForeground ()
		;
		fAlertAgent = new ContinualAlertAgent ( fNotifier );
	}

	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs clp ) throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException
	{
		return new ConsoleLooper (
			new String[] { "Notify Console" },
			"> ",
			". ",
			new CommandSet ()
		);
	}

	private class CommandSet extends StdCommandList 
	{
		public CommandSet ()
		{
			registerCommand ( new SubjCondCommand ( "notify" )
			{
				@Override
				protected ConsoleLooper.InputResult execute ( HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo ) throws UsageException, NvReadable.MissingReqdSettingException
				{
					final String subject = p.getString ( kSubject );
					final String condition = p.getString ( kCondition );

					fNotifier
						.onSubject ( subject )
						.withCondition ( condition )
						.send ()
					;

					return ConsoleLooper.InputResult.kReady;
				}
			} );

			registerCommand ( new SubjCondCommand ( "onset" )
			{
				@Override
				protected void setupParser ( CmdLineParser clp )
				{
					super.setupParser ( clp );
					clp.registerOptionWithValue ( kData, "d" );
				}

				@Override
				protected ConsoleLooper.InputResult execute ( HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo ) throws UsageException, NvReadable.MissingReqdSettingException
				{
					final String subject = p.getString ( kSubject );
					final String condition = p.getString ( kCondition );
					final String data = p.getString ( kData, null );

					final Alert a = fAlertAgent.onset ( subject, condition,
						( data == null ? null : JsonUtil.readJsonObject ( data ) )
					);
					outputAlert ( a, outTo );

					return ConsoleLooper.InputResult.kReady;
				}
			} );

			registerCommand ( new SubjCondCommand ( "clear" )
			{
				@Override
				protected ConsoleLooper.InputResult execute ( HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo ) throws UsageException, NvReadable.MissingReqdSettingException
				{
					final String subject = p.getString ( kSubject );
					final String condition = p.getString ( kCondition );

					final Alert a = fAlertAgent.clear ( subject, condition );
					outputAlert ( a, outTo );

					return ConsoleLooper.InputResult.kReady;
				}
			} );

			registerCommand ( new SimpleCommand ( "standing" )
			{
				@Override
				protected ConsoleLooper.InputResult execute ( HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo ) throws UsageException, NvReadable.MissingReqdSettingException
				{
					for ( Alert a : fAlertAgent.standingAlerts () )
					{
						outputAlert ( a, outTo );
					}

					return ConsoleLooper.InputResult.kReady;
				}
			} );
		}

		protected void outputAlert ( Alert a, PrintStream outTo )
		{
			if ( a == null )
			{
				outTo.println ( "No alert." );
			}
			else
			{
				outTo.println ( a.toString () );
			}
		}
	};

	private final ContinualNotifier fNotifier;
	private final ContinualAlertAgent fAlertAgent;

	private static final String kSubject = "subject";
	private static final String kCondition = "condition";
	private static final String kData = "data";

	private abstract class SubjCondCommand extends SimpleCommand
	{
		public SubjCondCommand ( String msg ) { super(msg); }

		@Override
		protected void setupParser ( CmdLineParser clp )
		{
			clp.registerOptionWithValue ( kSubject, "s" );
			clp.registerOptionWithValue ( kCondition, "c" );
		}
	};
	
	public static void main ( String[] args )
	{
		try
		{
			final NotifyConsole nc = new NotifyConsole ();
			nc.runFromMain ( args );
		}
		catch ( UsageException | LoadException | MissingReqdSettingException | InvalidSettingValueException | StartupFailureException e )
		{
			e.printStackTrace();
		}
	}
}
