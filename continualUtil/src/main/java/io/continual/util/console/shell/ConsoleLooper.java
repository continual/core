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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConsoleLineReader;
import io.continual.util.console.ConsoleProgram;
import io.continual.util.console.ConsoleProgram.UsageException;

import io.continual.util.nv.NvReadable;

/**
 * implements a looper that repeatedly prompts for a command line and handles it
 */
public class ConsoleLooper implements ConsoleProgram.Looper
{
	public static class Builder
	{
		public Builder () {}

		public Builder withMainPrompt ( String prompt ) { fPrompt = prompt; return this; }
		public Builder withSecondaryPrompt ( String prompt ) { fSecondaryPrompt = prompt; return this; }
		public Builder withPrompts ( String prompt, String secondary ) { fPrompt=prompt; fSecondaryPrompt = secondary; return this; }

		public Builder presentHeaderLine ( String line )
		{
			fHeaderLines.add ( line );
			return this;
		}

		public Builder presentHeaderLines ( List<String> lines )
		{
			fHeaderLines.addAll ( lines );
			return this;
		}

		public Builder addCommand ( Command c )
		{
			fCommands.registerCommand ( c );
			return this;
		}

		public Builder putObjectInWorkspace ( String key, Object o )
		{
			fObjects.put ( key, o );
			return this;
		}

		public ConsoleLooper build ()
		{
			final ConsoleLooper result = new ConsoleLooper ( fHeaderLines.toArray ( new String[fHeaderLines.size ()] ), fPrompt, fSecondaryPrompt, fCommands );
			for ( Map.Entry<String,Object> obj : fObjects.entrySet () )
			{
				result.getWorkspace ().put ( obj.getKey (), obj.getValue () );
			}
			return result;
		}

		private ArrayList<String> fHeaderLines = new ArrayList<String> ();
		private String fPrompt = "> ";
		private String fSecondaryPrompt = ". ";
		private StdCommandList fCommands = new StdCommandList ();
		private HashMap<String,Object> fObjects = new HashMap<> ();
	}
	
	public ConsoleLooper ( String[] headerLines, String prompt, String secondaryPrompt, CommandList cl )
	{
		fHeaderLines = headerLines;
		fInputQueue = new LinkedList<String> ();
		fPrompt = prompt;
		fSecondaryPrompt = secondaryPrompt;
		fEnableHelp = true;
		fWorkspace = new HashMap<String,Object> ();
		fState = InputResult.kReady;
		fCommands = cl;
	}

	public enum InputResult
	{
		kReady,
		kSecondaryPrompt,
		kQuit
	};

	/**
	 * this key is used for a boolean setting that will suppress the copyright notice 
	 */
	public static final String kSetting_Quiet = "quiet";

	@Override
	public boolean setup ( NvReadable p, CmdLinePrefs clp )
	{
		boolean quiet = p.getBoolean ( kSetting_Quiet, false );

		if ( clp != null )
		{
			final String args = clp.getFileArgumentsAsString ();
			if ( args != null && args.length () > 0 )
			{
				queue ( args );
				queue ( "quit" );
				quiet = true;
			}
		}

		if ( !quiet )
		{
			writeHeader ();
		}

		return true;
	}

	@Override
	public void teardown ( NvReadable p )
	{
	}

	@Override
	public boolean loop ( NvReadable p )
	{
		boolean result = true;
		try
		{
			final String line = getInput ();
			if ( line != null )
			{
				fState = handleInput ( p, line, System.out );
				if ( fState == null )
				{
					fState = InputResult.kReady;
				}
			}
			else
			{
				// end of input stream
				result = false;
			}
		}
		catch ( IOException x )
		{
			// a break in console IO, we're done
			System.err.println ( x.getMessage () );
			result = false;
		}
		return result && !fState.equals ( InputResult.kQuit );
	}

	public synchronized void queue ( String input )
	{
		fInputQueue.add ( input );
	}

	public synchronized void queueFromCmdLine ( CmdLinePrefs clp, boolean withQuit )
	{
		final Vector<String> args = clp.getFileArguments ();
		if ( args.size () > 0 )
		{
			final StringBuffer sb = new StringBuffer ();
			for ( String s : args )
			{
				sb.append ( s );
				sb.append ( ' ' );
			}
			queue ( sb.toString () );
			if ( withQuit ) queue ( "quit" );
		}
	}

	protected void writeHeader ()
	{
		if ( fHeaderLines != null )
		{
			for ( String header : fHeaderLines )
			{
				System.out.println ( header );
			}
		}
	}

	/**
	 * override this to handle input before its been parsed in the usual way
	 * @param p settings
	 * @param input the input string
	 * @param outputTo the output for commands to write into
	 * @return true to continue, false to exit
	 */
	protected InputResult handleInput ( NvReadable p, String input, PrintStream outputTo )
	{
		InputResult result = InputResult.kReady;

		final String[] commandLine = splitLine ( input );
		if ( commandLine.length == 0 )
		{
			result = handleEmptyLine ( p, outputTo );
		}
		else
		{
			result = handleCommand ( p, commandLine, outputTo );
		}

		return result;
	}

	/**
	 * default handling for empty lines -- just ignore them
	 * @param p the settings
	 * @param outputTo the output stream for commands
	 * @return true
	 */
	protected InputResult handleEmptyLine ( NvReadable p, PrintStream outputTo )
	{
		return InputResult.kReady;
	}

	/**
	 * consoles can override this to change how command lines are processed
	 * @param p the settings
	 * @param commandLine the command line componennts
	 * @param outputTo the output stream
	 * @return true to continue, false to exit
	 */
	protected InputResult handleCommand ( NvReadable p, String[] commandLine, PrintStream outputTo )
	{
		InputResult result = InputResult.kReady;
		final Command m = getHandler ( commandLine ); 
		if ( m != null )
		{
			final int argsLen = commandLine.length - 1;
			final String[] args = new String [ argsLen ];
			System.arraycopy ( commandLine, 1, args, 0, argsLen );

			try
			{
				result = invoke ( m, p, args, outputTo );
			}
			catch ( Exception x )
			{
				result = handleInvocationException ( commandLine, x, outputTo );
			}
		}
		else
		{
			result = handleUnrecognizedCommand ( commandLine, outputTo );
		}
		return result;
	}

	protected InputResult invoke ( Command m, NvReadable p, String[] args, PrintStream outputTo )
	{
		try
		{
			m.checkArgs ( p, args );
			return m.execute ( fWorkspace, outputTo );
		}
		catch ( UsageException x )
		{
			outputTo.println ( m.getUsage () );
			outputTo.println ( x.getMessage () );
		}
		return InputResult.kReady;
	}
	
	/**
	 * default handling for unrecognized commands
	 * @param commandLine the command line componennts
	 * @param outputTo the output stream
	 * @return inResult.kReady
	 */
	protected InputResult handleUnrecognizedCommand ( String[] commandLine, PrintStream outputTo )
	{
		outputTo.println ( "Unrecognized command '" + commandLine[0] + "'." );
		return InputResult.kReady;
	}

	/**
	 * default handling for invocation problems
	 * @param commandLine the command line componennts
	 * @param x an exception
	 * @param outputTo the output stream
	 * @return inResult.kReady
	 */
	protected InputResult handleInvocationException ( String[] commandLine, Exception x, PrintStream outputTo )
	{
		if ( x instanceof InvocationTargetException )
		{
			InvocationTargetException itc = (InvocationTargetException) x;
			Throwable target = itc.getTargetException ();
			outputTo.println ( "ERROR: " + target.getClass ().getName () + ": " + target.getMessage () );
		}
		else
		{
			outputTo.println ( "Error running command '" + commandLine[0] + "'. " + x.getMessage() );
		}
		return InputResult.kReady;
	}

	protected HashMap<String,Object> getWorkspace ()
	{
		return fWorkspace;
	}
	
	private String[] fHeaderLines;
	private final LinkedList<String> fInputQueue;
	private final String fPrompt;
	private final String fSecondaryPrompt;
	private boolean fEnableHelp;
	private InputResult fState;
	private final CommandList fCommands;
	private final HashMap<String,Object> fWorkspace;

	public static final String kCmdPrefix = "__";
	public static final int kCmdPrefixLength = kCmdPrefix.length ();

	private synchronized String getInput () throws IOException
	{
		String input = null;
		if ( fInputQueue.size () > 0 )
		{
			input = fInputQueue.remove ();
		}
		else
		{
			String prompt = fPrompt;
			if ( fState == InputResult.kReady )
			{
				System.out.println ();
			}
			else
			{
				prompt = fSecondaryPrompt;
			}

			input = ConsoleLineReader.getLine ( prompt );
		}
		return input;
	}

	/**
	 * split a string on its whitespace into individual tokens
	 * @param line the input string
	 * @return split array
	 */
	static String[] splitLine ( final String line )
	{
		final LinkedList<String> tokens = new LinkedList<String> ();

		StringBuffer current = new StringBuffer ();
		boolean quoting = false;
		boolean lastWasWs = true;
		for ( int i=0; i<line.length (); i++ )
		{
			final char c = line.charAt ( i );
			if ( Character.isWhitespace ( c ) && !quoting )
			{
				if ( current.length () > 0 )
				{
					tokens.add ( current.toString () );
				}
				current = new StringBuffer ();
				lastWasWs = true;
			}
			else if ( c == '"' )
			{
				if ( lastWasWs )
				{
					// starting quoted string. eat it, flip quote flag
					quoting = true;
				}
				else if ( !quoting )
				{
					// e.g. abc"def
					current.append ( c );
				}
				else
				{
					// end quoted string
					// e.g. "foo" bar, but also "foo"bar
					tokens.add ( current.toString () );
					current = new StringBuffer ();
					quoting = false;
				}
				lastWasWs = false;
			}
			else
			{
				current.append ( c );
				lastWasWs = false;
			}
		}
		if ( current.length () > 0 )
		{
			tokens.add ( current.toString () );
		}
		
		return tokens.toArray ( new String[ tokens.size () ] );
	}

	private Command getHandler ( String[] cmdLine )
	{
		if ( cmdLine.length > 0 )
		{
			return fCommands.getCommandFor ( cmdLine[0] );
		}
		else
		{
			return null;
		}
	}

	public void __script ( String[] args, PrintStream outTo ) throws ConsoleProgram.UsageException, IOException
	{
		if ( args.length != 2 )
		{
			throw new ConsoleProgram.UsageException ( "script <file>" );
		}

		LinkedList<String> lines = new LinkedList<String> ();
		final String filename = args[1];
		final BufferedReader bis = new BufferedReader ( new FileReader ( filename ) );
		String input;
		while ( ( input = bis.readLine () ) != null )
		{
			lines.add ( input );
		}
		bis.close ();

		// add to the front of the queue so that script within script runs in
		// correct order.
		synchronized ( this )
		{
			fInputQueue.addAll ( 0, lines );
		}
	}

	public void __help ( String[] args, PrintStream outTo ) throws ConsoleProgram.UsageException, IOException
	{
		if ( fEnableHelp )
		{
			TreeSet<String> allMethods = new TreeSet<String> ();

			Class<?> clazz = getClass ();
			while ( !clazz.equals ( Object.class ) )
			{
				Method[] methods = clazz.getDeclaredMethods ();
				for ( Method m : methods )
				{
					final String methodName = m.getName ();
					if ( methodName.startsWith ( kCmdPrefix ) &&
						methodName.length () > kCmdPrefixLength )
					{
						Class<?>[] params = m.getParameterTypes ();
						if ( params.length == 2 && params[0].equals ( String[].class ) && 
							params[1].equals ( PrintStream.class ) )
						{
							allMethods.add ( methodName.substring ( kCmdPrefixLength ) );
						}
					}
				}
				clazz = clazz.getSuperclass ();
			}

			for ( String s : allMethods )
			{
				outTo.println ( "    " + s );
			}
		}
	}
}
