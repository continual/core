package io.continual.browserDriver.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.json.JSONObject;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConfiguredConsole;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class LogProcessor extends ConfiguredConsole
{
	public interface LogRecordHandler
	{
		void handle ( JSONObject record, PrintStream out, PrintStream err );
		void cleanup ( PrintStream out, PrintStream err );
	}
	
	@Override
	protected ConfiguredConsole setupOptions ( CmdLineParser p )
	{
		p.registerOptionWithValue ( "handlerClass" );
		p.registerOptionWithValue ( "hostPart" );	// FIXME: specific to a record handler
		return super.setupOptions ( p );
	}

	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs cmdLine ) throws NvReadable.MissingReqdSettingException, NvReadable.InvalidSettingValueException, StartupFailureException 
	{
		try
		{
			final LogRecordHandler lp = Builder.withBaseClass ( LogRecordHandler.class )
				.usingData ( p )
				.usingClassName ( p.get ( "handlerClass" ) )
				.searchingPath ( LogProcessor.class.getPackage ().getName () )
				.build ()
			;
	
			final PrintStream out = System.out;
			final PrintStream err = System.err;
	
			for ( String filename : cmdLine.getFileArguments () )
			{
				try (
					final FileInputStream fis = new FileInputStream ( new File ( filename ) );
					final BufferedReader br = new BufferedReader ( new InputStreamReader ( fis ) );
				)
				{
					while ( br.ready () )
					{
						final String timestamp = br.readLine ();
		
						// now read the JSON entry until the separator
						final StringBuilder sb = new StringBuilder ();
						while ( true )
						{
							final String line = br.readLine ();
							if ( line.equals ( "----" ))
							{
								break;
							}
							sb.append ( line ).append ( System.lineSeparator () );
						}
		
						final JSONObject data = new JSONObject ( new CommentedJsonTokener ( sb.toString () ) );
						data.put ( "timestamp", timestamp );
		
						lp.handle ( data, out, err );
					}
				}
				catch ( IOException x )
				{
					err.println ( x.getMessage() );
				}
			}

			// cleanup
			lp.cleanup ( out, err );
		}
		catch ( BuildFailure e )
		{
			throw new StartupFailureException ( e );
		}
		return null;
	}

	public static void main ( String[] args )
	{
		try
		{
			new LogProcessor ().runFromMain ( args );
		}
		catch ( UsageException | LoadException | MissingReqdSettingException
			| InvalidSettingValueException | StartupFailureException e )
		{
			System.err.println ( e.getMessage() );
		}
	}
}
