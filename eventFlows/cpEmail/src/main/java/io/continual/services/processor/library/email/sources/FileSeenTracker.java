package io.continual.services.processor.library.email.sources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.util.time.Clock;

public class FileSeenTracker implements SeenTracker
{
	public FileSeenTracker ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		try
		{
			fFile = new File ( sc.getExprEval ().evaluateText ( config.getString ( "file" ) ) );
			if ( !fFile.exists () )
			{
				fFile.createNewFile ();
			}
			fData = new HashMap<>();
			fAgeThresholdMs = 1000L * config.optLong ( "ageThresholdSeconds", 60*60*24*7 );

			read ();
		}
		catch ( IOException x )
		{
			throw new BuildFailure ( x );
		}
	}

	@Override
	public void close ()
	{
		writeAll ();
		if ( fAppender != null ) fAppender.close ();
	}

	@Override
	public synchronized void addUid ( long uid )
	{
		final Entry e = new Entry ( uid, Clock.now () );
		fData.put ( uid, e );
		fMaxUid = Math.max ( uid, fMaxUid );
		append ( e );
	}

	@Override
	public synchronized boolean isUidSeen ( long uid )
	{
		return uid <= fMaxUid || fData.containsKey ( uid );
	}

	private final File fFile;
	private final long fAgeThresholdMs;
	private final HashMap<Long,Entry> fData;
	private long fMaxUid;
	private PrintWriter fAppender;

	private static class Entry
	{
		public Entry ( long uid, long timestampMs )
		{
			fUid = uid;
			fTsMs = timestampMs;
		}

		private final long fUid;
		private final long fTsMs;
	}

	private void read () throws IOException
	{
		final long nowMs = Clock.now ();
		fMaxUid = -1L;
		
		if ( fAppender != null ) fAppender.close ();

		try ( BufferedReader br = new BufferedReader ( new FileReader ( fFile ) ) )
		{
			fData.clear ();
			String line = null;
			while ( null != ( line = br.readLine () ) )
			{
				final String[] parts = line.split ( "," );
				if ( parts.length != 2 )
				{
					log.warn ( "Ignored: " + line );
				}
				final long uid = Long.parseLong ( parts[0] );
				final long tsms = Long.parseLong ( parts[1] );
				if ( tsms + fAgeThresholdMs >= nowMs )
				{
					fData.put ( uid, new Entry ( uid, tsms ) );
				}
				fMaxUid = Math.max ( uid, fMaxUid );
			}
		}

		fAppender = new PrintWriter ( new FileWriter ( fFile, true ) );
	}

	private void append ( Entry e )
	{
		write ( fAppender, e );
	}

	// FIXME: probably just need to write out max...

	private void write ( PrintWriter pw, Entry e )
	{
		pw.println ( Long.toString ( e.fUid ) + "," + Long.toString ( e.fTsMs ) );
		pw.flush ();
	}

	private void writeAll ()
	{
		if ( fAppender != null ) fAppender.close ();

		final long nowMs = Clock.now ();
		try ( PrintWriter pw = new PrintWriter ( fFile ) )
		{
			for ( Map.Entry<Long,Entry> e : fData.entrySet () )
			{
				if ( e.getValue ().fTsMs + fAgeThresholdMs >= nowMs )
				{
					write ( pw, e.getValue () );
				}
			}

			fAppender = new PrintWriter ( new FileWriter ( fFile, true ) );
		}
		catch ( IOException x )
		{
			log.warn ( "Failed writing data file: " + x.getMessage () );
		}
	}
	
	private static final Logger log = LoggerFactory.getLogger ( FileSeenTracker.class );
}
