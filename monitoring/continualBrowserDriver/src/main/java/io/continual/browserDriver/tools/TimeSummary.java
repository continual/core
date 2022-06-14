package io.continual.browserDriver.tools;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.util.data.csv.CsvLineBuilder;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.nv.NvReadable;

public class TimeSummary implements LogProcessor.LogRecordHandler
{
	public static TimeSummary fromSettings ( NvReadable settings )
	{
		return new TimeSummary ();
	}

	@Override
	public void handle ( JSONObject record, final PrintStream out, final PrintStream err )
	{
		// like 2018-01-17T22:03:08.336Z
		final SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy-MM-dd'T'hh:mm:ss.SSS'Z'" );

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
								final CsvLineBuilder csv = new CsvLineBuilder ();

								final String start = entry.getString ( "startedDateTime" );
								final Date startDate = sdf.parse ( start );
								csv
									.append ( startDate.getTime () )
									.append ( start )
								;

								final JSONObject req = entry.getJSONObject ( "request" );
								final String reqMethod = req.getString ( "method" );
								final String reqUrl = req.getString ( "url" );
								final URL url = new URL ( reqUrl );
								
								csv
									.append ( reqMethod )
									.append ( reqUrl )
									.append ( url.getHost () )
									.append ( url.getProtocol () + "://" + url.getHost () + url.getPath () )
								;

								csv.append ( entry.optString ( "serverIPAddress", "" ) );

								final JSONObject resp = entry.getJSONObject ( "response" );
								csv.append ( "" + resp.getInt ( "status" ) + " " + resp.getString ( "statusText" ) );

								csv
									.append ( entry.getLong ( "time" ) )
									.append ( entry.getJSONObject ( "timings" ).getLong ( "wait" ) )
								;
								
								out.println ( csv.toString() );
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
	}
}
