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
import java.util.HashMap;
import java.util.Map;

import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConsoleProgram.UsageException;

import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public abstract class SimpleCommand implements Command
{
	protected SimpleCommand ( String cmd )
	{
		this ( cmd, cmd, null );
	}

	protected SimpleCommand ( String cmd, String usage )
	{
		this ( cmd, usage, null );
	}

	protected SimpleCommand ( String cmd, String usage, String help )
	{
		fCmd = cmd;
		fUsage = usage;
		fHelp = help;
		fArgsParser = new CmdLineParser ();
		fPrefs = null;
		fEnabled = true;
	}

	public void enable ( boolean e )
	{
		fEnabled = e;
	}

	public boolean enabled ()
	{
		return fEnabled;
	}

	@Override
	public final void checkArgs ( NvReadable basePrefs, String[] args ) throws UsageException
	{
		setupParser ( fArgsParser );
		fPrefs = fArgsParser.processArgs ( args );
	}

	@Override
	public String getCommand () { return fCmd; }

	@Override
	public String getUsage () { return fUsage; }

	@Override
	public String getHelp () { return fHelp; }

	@SuppressWarnings("unchecked")
	public static <T> T getWorkspaceObject ( Map<String,Object> map, String key, Class<T> clazz )
	{
		final Object val = map.get ( key );
		if ( clazz.isInstance ( val ) )
		{
			return (T) val;
		}
		return null;
	}
	
	@Override
	public final ConsoleLooper.InputResult execute ( HashMap<String,Object> workspace, PrintStream outTo ) throws UsageException
	{
		try
		{
			return execute ( workspace, fPrefs, outTo );
		}
		catch ( NvReadable.MissingReqdSettingException e )
		{
			throw new UsageException ( e );
		}
	}

	/**
	 * Override this to run the command. 
	 * @param workspace
	 * @param p
	 * @param outTo
	 * @return true to continue, false to exit
	 * @throws UsageException 
	 * @throws NvReadable.MissingReqdSettingException 
	 */
	protected abstract ConsoleLooper.InputResult execute ( HashMap<String,Object> workspace, CmdLinePrefs p, PrintStream outTo ) throws UsageException, MissingReqdSettingException;

	/**
	 * override this to specify arguments for the command
	 * @param clp
	 */
	protected void setupParser ( CmdLineParser clp ) {}

	private final String fCmd;
	private final String fUsage;
	private final String fHelp;

	private final CmdLineParser fArgsParser;
	private CmdLinePrefs fPrefs;
	private boolean fEnabled;
}
