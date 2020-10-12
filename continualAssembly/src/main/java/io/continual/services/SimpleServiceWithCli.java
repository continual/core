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

package io.continual.services;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import io.continual.services.SimpleService;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.shell.Command;
import io.continual.util.console.shell.ConsoleLooper.InputResult;
import io.continual.util.console.shell.SimpleCommand;

/**
 * A simple service implementation that also includes CLI support. 
 */
public abstract class SimpleServiceWithCli extends SimpleService implements CliControlledService
{
	protected SimpleServiceWithCli ()
	{
		fCmds = new HashMap<String,Command> ();
	}

	protected SimpleServiceWithCli register ( Command cmd )
	{
		fCmds.put ( cmd.getCommand (), cmd );
		
		return this;
	}

	@Override
	public Command getCommandFor ( String cmd )
	{
		Command result = fCmds.get ( cmd );
		if ( result == null && cmd.equals ( "help" ) )
		{
			result = new SimpleCommand ( "help" )
			{
				@Override
				protected InputResult execute ( HashMap<String, Object> workspace, CmdLinePrefs p, PrintStream outTo )
				{
					final LinkedList<String> cmds = new LinkedList<String> ( fCmds.keySet () );
					Collections.sort ( cmds );
					for ( String cmd : cmds )
					{
						outTo.println ( cmd );
					}
					return InputResult.kReady;
				}
			};
		}
		return result;
	}

	private final HashMap<String,Command> fCmds;
}
