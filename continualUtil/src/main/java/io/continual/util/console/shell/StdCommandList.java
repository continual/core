/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package io.continual.util.console.shell;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;

public class StdCommandList implements CommandList
{
	public StdCommandList ()
	{
		this ( true );
	}

	public StdCommandList ( boolean withStdCommands )
	{
		fCommands = new HashMap<String,Command> ();
		if ( withStdCommands )
		{
			addStandardCommands ();
		}
	}

	public void registerCommand ( Command c )
	{
		fCommands.put ( c.getCommand (), c );
	}

	public void removeCommand ( String key )
	{
		fCommands.remove ( key );
	}

	public void clearCommands ()
	{
		fCommands.clear ();
	}

	public void addStandardCommands ()
	{
		registerCommand ( new SimpleCommand ( "exit" )
		{
			@Override
			public ConsoleLooper.InputResult execute ( HashMap<String,Object> workspace, CmdLinePrefs prefs, PrintStream outTo ) { return ConsoleLooper.InputResult.kQuit; }
		} );
		registerCommand ( new SimpleCommand ( "quit" )
		{
			@Override
			public ConsoleLooper.InputResult execute ( HashMap<String,Object> workspace, CmdLinePrefs prefs, PrintStream outTo ) { return ConsoleLooper.InputResult.kQuit; }
		} );
		registerCommand ( new SimpleCommand ( "help", "help [<command>]" )
		{
			@Override
			protected void setupParser ( CmdLineParser clp )
			{
				clp.requireFileArguments ( 0, 1 );
			}

			@Override
			public ConsoleLooper.InputResult execute ( HashMap<String,Object> workspace, CmdLinePrefs prefs, PrintStream outTo )
			{
				if ( prefs.getFileArguments ().size() == 1 )
				{
					final String cmdText = prefs.getFileArguments ().firstElement ();
					final Command cmd = fCommands.get ( cmdText );
					if ( cmd != null )
					{
						outTo.println ( "    " + cmd.getUsage () );
						final String help = cmd.getHelp ();
						if ( help != null )
						{
							outTo.println ();
							outTo.println ( help );
						}
					}
					else
					{
						outTo.println ( "Unknown command: " + cmdText + "." );
					}
				}
				else
				{
					outTo.println  ( "The available commands are:" );
					outTo.println  ();
					
					final LinkedList<String> commands = new LinkedList<String> ();
					commands.addAll ( getAllCommands () );
					Collections.sort ( commands );
					for ( String cmd : commands )
					{
						outTo.println ( "    " + cmd );
					}

					outTo.println  ();
					outTo.println  ( "You can type 'help <command>' to get more info on that command." );
				}
				return ConsoleLooper.InputResult.kReady;
			}
		} );
	}

	public Collection<String> getAllCommands ()
	{
		return fCommands.keySet ();
	}

	@Override
	public Command getCommandFor ( String cmd )
	{
		return fCommands.get ( cmd );
	}

	private final HashMap<String,Command> fCommands;
}
