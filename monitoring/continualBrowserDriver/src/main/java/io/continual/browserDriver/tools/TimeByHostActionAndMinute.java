package io.continual.browserDriver.tools;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.util.data.csv.CsvLineBuilder;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class TimeByHostActionAndMinute implements LogProcessor.LogRecordHandler
{
	public static TimeByHostActionAndMinute fromSettings ( NvReadable settings ) throws MissingReqdSettingException
	{
		return new TimeByHostActionAndMinute ( settings );
	}

	private static class Stats
	{
		public Stats ()
		{
			fTotal = 0;
		}

		public void addTiming ( String host, String action, String timeBucket, long duration )
		{
			fTotal += duration;
			fHost = host;
			fAction = action;
			fTimeBucket = timeBucket;
		}

		long fTotal;
		String fHost;
		String fAction;
		String fTimeBucket;
	}

	@Override
	public void handle ( JSONObject record, final PrintStream out, final PrintStream err )
	{
		final SimpleDateFormat dateBucketFormatter = new SimpleDateFormat ( "yyyy-MM-dd HH:mm" );

		JsonVisitor.forEachElement ( record.optJSONArray ( "actions" ), new ArrayVisitor<JSONObject,JSONException> ()
		{
			@Override
			public boolean visit ( JSONObject action )  throws JSONException 
			{
				final long startMs = action.getLong ( "startMs" );
				final long endMs = action.getLong ( "endMs" );
				final long durationMs = endMs - startMs;

				final String host = getHost ( action );
				final String actionName = action.getString ( "name" );
				final String bucketTime = dateBucketFormatter.format ( new Date ( startMs ) );

				final String bucket =host + " " + actionName + " " + bucketTime;

				Stats stats = fStats.get ( bucket );
				if ( stats == null )
				{
					stats = new Stats ();
					fStats.put ( bucket, stats );
				}
				stats.addTiming ( host, actionName, bucketTime, durationMs );

				return true;
			}
		} );
	}

	@Override
	public void cleanup ( PrintStream out, PrintStream err )
	{
		out.println ( "Action Host and Minute,Duration" );

		final LinkedList<String> sigs = new LinkedList <> ( fStats.keySet () );
		Collections.sort ( sigs );

		// generate report
		for ( String sig : sigs )
		{
			final Stats stats = fStats.get ( sig );

			final CsvLineBuilder csv = new CsvLineBuilder ();
			
			csv
				.append ( sig )
				.append ( stats.fHost )
				.append ( stats.fAction )
				.append ( stats.fTimeBucket )
				.append ( stats.fTotal )
			;
			
			out.println ( csv.toString () );
		}
	}

	private String getHost ( JSONObject action )
	{
		final JSONArray entries = action.getJSONObject ( "timing" ).getJSONObject ( "log" ).getJSONArray ( "entries" );
		for ( int i=0; i<entries.length (); i++ )
		{
			try
			{
				final JSONObject entry = entries.getJSONObject ( i );
				final JSONObject req = entry.getJSONObject ( "request" );
				final String reqUrl = req.getString ( "url" );
				final URL url = new URL ( reqUrl );
				if ( url.getHost ().contains ( fHostPart ) )
				{
					return url.getHost ();
				}
			}
			catch ( MalformedURLException x )
			{
				// skip it
			}
		}
		return "";
	}
	
	private TimeByHostActionAndMinute ( NvReadable settings ) throws MissingReqdSettingException
	{
		fStats = new HashMap<String,Stats> ();
		fHostPart = settings.getString ( "hostPart" );
	}

	private final String fHostPart;
	private final HashMap<String, Stats> fStats;
}
