package io.continual.services.processor.library.influxdb.common;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.util.data.exprEval.ExpressionEvaluator;

public class IdbConnection
{
	@SuppressWarnings("null")
	public IdbConnection ( JSONObject config, ExpressionEvaluator ee ) throws BuildFailure
	{
		try
		{
			fUrl = getValue ( ee, config, new String[] { "url", "host" }, "localhost:8086", true );
			final String token = getValue ( ee, config, new String[] { "token" }, null, true ); 

			final String org = getValue ( ee, config, new String[] { "org" }, null, true ); 
			final String bucket = getValue ( ee, config, new String[] { "bucket" }, null, true ); 

			if ( fUrl == null || token == null )
			{
				throw new BuildFailure ( "'url' and 'token' are required for IdbConnection." );
			}

//			fDbUser = config.optString ( "dbUser", null );
//			fDbPwd = config.optString ( "dbPwd", null );

			fDb = InfluxDBClientFactory.create ( fUrl, token.toCharArray (), org, bucket );

			log.info ( "Creating InfluxDB connection using URL {}", fUrl );
		}
		catch ( JSONException x )
		{
			throw new BuildFailure ( x );
		}
	}

	public void close ()
	{
		if ( fDb != null )
		{
			fDb.close ();
		}
	}
	
	public InfluxDBClient getDb () { return fDb; }

	private final String fUrl;
//	private final String fDbUser;
//	private final String fDbPwd;
	private InfluxDBClient fDb;

	private static final Logger log = LoggerFactory.getLogger ( IdbConnection.class );

	private static String getValue ( ExpressionEvaluator ee, JSONObject config, String[] keys, String def, boolean reqd ) throws BuildFailure
	{
		for ( String key : keys )
		{
			String val = config.optString ( key, null );
			if ( val != null )
			{
				return ee == null ? val : ee.evaluateText ( val );
			}
		}
		
		if ( def != null )
		{
			return ee == null ? def : ee.evaluateText ( def );
		}
		
		if ( reqd )
		{
			throw new BuildFailure ( "Missing required setting for [" + keys[0] + "]." );
		}
		return null;
	}
}
