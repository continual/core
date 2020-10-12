package io.continual.services.processor.library.jdbcio.common;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.library.jdbcio.DbConnection;
import io.continual.util.data.StringUtils;

public class DbConnector
{
	public static DbConnection dbConnectionFromConfig ( JSONObject config ) throws BuildFailure
	{
		// the config can have either a url or a host + dbName, which we'll use to construct the URL
		
		String url = getValue ( config, "url", false );
		if ( url == null )
		{
			final String host = getValue ( config, "host", true );
			final String dbname = getValue ( config, "name", true );
			url = "jdbc:mysql://" + host + "/" + dbname + "?serverTimezone=UTC&rewriteBatchedStatements=true&useSSL=false&autoReconnect=true";
		}

		final String user = getValue ( config, "user", false );
		final String pwd = getValue ( config, "password", false );

		return new DbConnection ( url, user, pwd );
	}
	
	public DbConnector ( JSONObject config ) throws BuildFailure
	{
		this ( dbConnectionFromConfig ( config ), config );
	}

	public DbConnector ( DbConnection dbsrc, JSONObject config ) throws BuildFailure
	{
		try
		{
			fDb = dbsrc;
			fTable = config.optString ( "table", null );
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

	private static String getValue ( JSONObject config, String key, boolean reqd ) throws BuildFailure
	{
		String val = config.optString ( key, null );
		if ( val == null )
		{
			val = config.optString ( "db" + StringUtils.toFirstUpper ( key ), null );
		}
		if ( val == null && reqd )
		{
			throw new BuildFailure ( "Missing required setting for [" + key + "]." );
		}
		return val;
	}
}
