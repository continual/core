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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvWriteable;
import io.continual.util.nv.impl.nvEnvProperties;
import io.continual.util.nv.impl.nvInstallTypeWrapper;
import io.continual.util.nv.impl.nvReadableStack;
import io.continual.util.nv.impl.nvWriteableTable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

/**
 * A console program runs on the command line.
 * <p>
 * The console class expects the program's main() routine to call its
 * runFromMain() method to start the system. 
 */
public class ConsoleProgram
{
	public static class UsageException extends Exception
	{
		public UsageException ( String correctUsage ) { super(correctUsage); }
		public UsageException ( Exception cause ) { super(cause); }
		private static final long serialVersionUID = 1L;
	}

	public static class StartupFailureException extends Exception
	{
		public StartupFailureException ( Exception x ) { super(x); }
		public StartupFailureException ( String msg ) { super(msg); }
		public StartupFailureException ( String msg, Exception x ) { super(msg,x); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * A looper is an object that is run repeatedly (in a loop). This class is
	 * what the main thread of the program does between startup and exit.
	 */
	public interface Looper
	{
		/**
		 * setup the looper and return true to continue. Called once.
		 * @param prefs
		 * @param cmdLine
		 * @return true/false
		 */
		boolean setup ( NvReadable prefs, CmdLinePrefs cmdLine );

		/**
		 * Run a loop iteration, return true to continue, false to exit. (Note 
		 * that nothing requires this implementation to do a small amount of
		 * work vs. lengthy processing.)
		 * @return true to continue, false to exit
		 */
		boolean loop ( NvReadable prefs );

		/**
		 * teardown the looper. called once.
		 * @param prefs
		 */
		void teardown ( NvReadable prefs );
	}

	protected CmdLineParser getCmdLineParser ()
	{
		return fCmdLineParser;
	}

	protected ConsoleProgram ()
	{
		fHostInfo = new nvWriteableTable ();
		fDefaults = new nvWriteableTable ();
		fCmdLineParser = new CmdLineParser ();
	}

	public void runFromMain ( String[] args ) throws UsageException, LoadException, MissingReqdSettingException, InvalidSettingValueException, StartupFailureException
	{
		// get setup
		installShutdownHook ();
		setupHostInfo ();
		setupDefaults ( fDefaults );
		setupOptions ( fCmdLineParser );

		// parse the command line
		final CmdLinePrefs cmdLine = fCmdLineParser.processArgs ( args );

		// build a preferences stack
		final nvReadableStack stack = new nvReadableStack ();
		stack.push ( fHostInfo );					// sets 'hostname'
		stack.push ( new nvEnvProperties() );		// makes system environment available
		stack.push ( fDefaults );					// app defaults
		stack.push ( cmdLine );						// settings from command line

		// wrap the settings stack with the install-type suffix check,
		// which lets you set -Drr.installation=foo on the Java cmd line
		// and then have settings like mySetting[foo]=bar as specialization
		// over mySetting=theUsualValue.
		final nvInstallTypeWrapper wrapper = new nvInstallTypeWrapper ( stack ); 

		// optionally load more configuration from the app. If provided,
		// it fits in the stack below command line settings
		final NvReadable config = loadAdditionalConfig ( wrapper );
		if ( config != null )
		{
			stack.pushBelow ( config, cmdLine );
			wrapper.rescan ();
		}

		// init and get the run loop
		final Looper l = init ( wrapper, cmdLine );
		if ( l != null )
		{
			if ( l.setup ( wrapper, cmdLine ) )
			{
				while ( l.loop ( wrapper ) ) {}
				l.teardown ( wrapper );
			}
		}

		cleanup ();
	}

	/**
	 * Override this to handle an abrupt shutdown. This method is called when the system exits.
	 */
	protected void onShutdown () { }

	/**
	 * Override this to setup default settings for the program. 
	 * @param pt
	 */
	protected ConsoleProgram setupDefaults ( NvWriteable pt ) { return this; }

	/**
	 * Override this to setup recognized command line options. Note that default values
	 * provided to the command line reader are NOT available from init's settings instance.
	 * That's because the settings system doesn't have a way to differentiate between having
	 * a key and having a key as a default value. When stacked, if the command line parser
	 * states that it has a key, then any explicit setting further down the stack will not
	 * be used.
	 * @param p
	 */
	protected ConsoleProgram setupOptions ( CmdLineParser p ) { return this; }

	/**
	 * Override this to load additional configuration. If a non-null config is returned,
	 * it's inserted into the preferences stack between the default settings and the command line
	 * settings. That way, the command line arguments have precedence.
	 * @param currentPrefs
	 * @throws NvReadable.LoadException 
	 * @throws NvReadable.MissingReqdSettingException 
	 */
	protected NvReadable loadAdditionalConfig ( NvReadable currentPrefs ) throws LoadException, MissingReqdSettingException { return null; }

	/**
	 * Init the program and return a loop instance if the program should continue. The base
	 * class returns null, so you have to override this to do anything beyond init.
	 * @param p settings
	 * @param cmdLine command line values
	 * @return non-null to continue, null to exit
	 * @throws NvReadable.MissingReqdSettingException
	 * @throws NvReadable.InvalidSettingValueException 
	 */
	protected Looper init ( NvReadable p, CmdLinePrefs cmdLine ) throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException { return null; }

	/**
	 * Override this to run any cleanup code after the main loop.
	 */
	protected void cleanup () {}

	/**
	 * expand a file argument ("*" matches, etc.)
	 * @param arg
	 * @return
	 * @throws FileNotFoundException
	 */
	protected List<File> expandFileArg ( String arg ) throws FileNotFoundException
	{
		final LinkedList<File> fileList=  new LinkedList<File> ();

		final File file = new File ( arg );
		final File parentDir = file.getParentFile ();
		if ( parentDir != null )
		{
			final String matchPart = file.getName ().replace ( "*", ".*" );	// cmd line regex to java regex
			final Pattern p = Pattern.compile ( matchPart );
	
			final File[] files = parentDir.listFiles ( new FilenameFilter ()
			{
				@Override
				public boolean accept ( File dir, String name )
				{
					return p.matcher ( name ).matches ();
				}
			} );
	
			if ( files != null )
			{
				for ( File f : files )
				{
					fileList.add ( f );
				}
			}
		}
		return fileList;
	}
	
	private final CmdLineParser fCmdLineParser;
	private final nvWriteableTable fDefaults;
	private final nvWriteableTable fHostInfo;

	private static final Logger log = LoggerFactory.getLogger ( ConsoleProgram.class );
	
	private void setupHostInfo ()
	{
		try
		{
			fHostInfo.set ( "hostname", InetAddress.getLocalHost().getHostName() );
		}
		catch ( UnknownHostException e )
		{
			log.warn ( "Couldn't establish hostname.", e );
		}
	}

	private void installShutdownHook ()
	{
		Runtime.getRuntime ().addShutdownHook (
			new Thread ()
			{
				@Override
				public void run ()
				{
					onShutdown ();
				}
			}
		);
	}
}
