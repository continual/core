package io.continual.browserDriver.tools;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.util.data.csv.CsvLineBuilder;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.nv.NvReadable;

public class TrxTimeByHostAndMinute implements LogProcessor.LogRecordHandler
{
	public static TrxTimeByHostAndMinute fromSettings ( NvReadable settings )
	{
		return new TrxTimeByHostAndMinute ();
	}

	private static class Stats
	{
		public Stats ()
		{
			fTotal = 0;
		}

		public void addTiming ( long at, long duration )
		{
			fTotal += duration;
		}

		long fTotal;
	}

	@Override
	public void handle ( JSONObject record, final PrintStream out, final PrintStream err )
	{
		// like 2018-01-17T22:03:08.336Z
		final SimpleDateFormat dateParser = new SimpleDateFormat ( "yyyy-MM-dd'T'hh:mm:ss.SSS'Z'" );
		final SimpleDateFormat dateBucketFormatter = new SimpleDateFormat ( "yyyy-MM-dd HH:mm" );
	
		JsonVisitor.forEachElement ( record.optJSONArray ( "actions" ), new ArrayVisitor<JSONObject,JSONException> ()
		{
			@Override
			public boolean visit ( JSONObject action )  throws JSONException 
			{
				JsonVisitor.forEachElement ( action.getJSONObject ( "timing" ).getJSONObject ( "log" ).getJSONArray ( "entries" ),
					new ArrayVisitor<JSONObject,JSONException> ()
					{
						@Override
						public boolean visit ( JSONObject entry ) throws JSONException
						{
							try
							{
								final JSONObject req = entry.getJSONObject ( "request" );
								final String reqUrl = req.getString ( "url" );
								final URL url = new URL ( reqUrl );

								final String start = entry.getString ( "startedDateTime" );
								final Date startDate = dateParser.parse ( start );
								final String bucketTime = dateBucketFormatter.format ( startDate );

								final String sig = url.getHost () + " " + bucketTime;

								Stats stats = fStats.get ( sig );
								if ( stats == null )
								{
									stats = new Stats ();
									fStats.put ( sig, stats );
								}
								stats.addTiming ( startDate.getTime (), entry.getLong ( "time" ) );
							}
							catch ( ParseException e )
							{
								throw new JSONException ( "Couldn't parse date. " + e.getMessage () );
							}
							catch ( MalformedURLException e )
							{
								throw new JSONException ( "Couldn't parse URL. " + e.getMessage () );
							}
							
							return true;
						}
					} );
				return true;
			}
		} );
	}

	@Override
	public void cleanup ( PrintStream out, PrintStream err )
	{
		out.println ( "Host and Minute,Duration" );

		final LinkedList<String> sigs = new LinkedList <> ( fStats.keySet () );
		Collections.sort ( sigs );

		// generate report
		for ( String sig : sigs )
		{
			final Stats stats = fStats.get ( sig );

			final CsvLineBuilder csv = new CsvLineBuilder ();
			
			csv
				.append ( sig )
				.append ( stats.fTotal )
			;
			
			out.println ( csv.toString () );
		}
	}

	private TrxTimeByHostAndMinute ()
	{
		fStats = new HashMap<String,Stats> ();
	}

	private final HashMap<String, Stats> fStats;
}
