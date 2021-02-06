package io.continual.services.processor.library.jdbcio.common;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.library.jdbcio.DbConnection;
import io.continual.util.data.exprEval.ExpressionEvaluator;

public class DbConnector
{
	public static DbConnection dbConnectionFromConfig ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		final ExpressionEvaluator ee = sc != null ? sc.getServiceContainer ().getExprEval ( config ) : new ExpressionEvaluator ();

		// the config can have either a url or a host + dbName, which we'll use to construct the URL
		
		String url = getValue ( ee, config, new String[] { "dbUrl", "url" }, false );
		if ( url == null || url.length () == 0 )
		{
			final String host = getValue ( ee, config, new String[] { "dbHost", "host" }, true );
			final String dbname = getValue ( ee, config, new String[] { "dbName", "name" }, true );

			if ( host == null || host.length () == 0 )
			{
				throw new BuildFailure ( "Neither 'dbUrl' nor 'dbHost' has a value." );
			}
			if ( dbname == null || dbname.length () == 0 )
			{
				throw new BuildFailure ( "Neither 'dbUrl' nor 'dbName' has a value." );
			}

			url = "jdbc:mysql://" + host + "/" + dbname + "?serverTimezone=UTC&rewriteBatchedStatements=true&useSSL=false&autoReconnect=true";
		}

		final String user = getValue ( ee, config, new String[] { "dbUser", "user" }, false );
		final String pwd = getValue ( ee, config, new String[] { "dbPassword", "password" }, false );

		return new DbConnection ( url, user, pwd );
	}
	
	public DbConnector ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( dbConnectionFromConfig ( sc, config ), sc, config );
	}

	public DbConnector ( DbConnection dbsrc, ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		try
		{
			fDb = dbsrc;
			fTable = sc.getServiceContainer ().getExprEval ( config ).evaluateText ( config.optString ( "table", null ) );
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	public DbConnection getDb () { return fDb; }
	public String getTable () { return fTable; }
	
	private final DbConnection fDb;
	private final String fTable;

	private static String getValue ( ExpressionEvaluator ee, JSONObject config, String[] keys, boolean reqd ) throws BuildFailure
	{
		for ( String key : keys )
		{
			String val = config.optString ( key, null );
			if ( val != null )
			{
				return ee.evaluateText ( val );
			}
		}
		if ( reqd )
		{
			throw new BuildFailure ( "Missing required setting for [" + keys[0] + "]." );
		}
		return null;
	}
}
