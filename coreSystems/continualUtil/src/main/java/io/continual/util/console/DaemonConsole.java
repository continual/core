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
package io.continual.util.console;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.legal.CopyrightGenerator;

import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

/**
 * A daemon program base class. It sets up commonly used program arguments, etc.
 * 
 * The actual class must implement daemonStillRunning. (If it doesn't, the system
 * will shutdown.)
 */
public class DaemonConsole extends ConfiguredConsole
{
	protected DaemonConsole ( String programName )
	{
		super ();
		fName = programName;
		fCopy = new CopyrightGenerator ();
	}

	protected String getProgramName ()
	{
		return fName;
	}

	protected DaemonConsole registerCopyrightHolder ( String holder, int startYear )
	{
		fCopy.addHolder ( holder, startYear );
		return this;
	}

	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs clp ) throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException
	{
		if ( !fQuiet )
		{
			System.out.println ( fName );
			for ( String notice : getCopyrightLines () )
			{
				System.out.println ( notice );
			}
		}

		return new BackgroundLooper ( 1000 * 3 )
		{
			@Override
			public boolean stillRunning ()
			{
				return daemonStillRunning ();
			}

			@Override
			public void teardown ( NvReadable p )
			{
				daemonShutdown ();
			}
		};
	}

	protected boolean daemonStillRunning ()
	{
		log.warn ( "The daemon class must implement daemonStillRunning(). (And don't call the base class implementation.)" );
		return false;
	}

	protected void daemonShutdown ()
	{
	}

	protected void quietStartup ()
	{
		fQuiet = true;
	}

	protected List<String> getCopyrightLines ()
	{
		return fCopy.getCopyrightNotices ();
	}

	private final String fName;
	private final CopyrightGenerator fCopy;
	private boolean fQuiet = false;
	private static final Logger log = LoggerFactory.getLogger ( DaemonConsole.class );
}
