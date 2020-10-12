package io.continual.services.processor.library.jdbcio;

import java.sql.Connection;
import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import io.continual.builder.Builder.BuildFailure;

public class DbConnection
{
	public DbConnection ( String url, String user, String pwd ) throws BuildFailure
	{
		this (
			new JSONObject ()
				.put ( "url", url )
				.put ( "dbUser", user )
				.put ( "dbPwd", pwd )
		);
	}

	public DbConnection ( JSONObject config ) throws BuildFailure
	{
		try
		{
			fUrl = config.getString ( "url" );
			fDbUser = config.optString ( "dbUser", null );
			fDbPwd = config.optString ( "dbPwd", null );
	
			log.info ( "Creating DB connection using URL {}", fUrl );
			
			fDbPool = new ComboPooledDataSource ();
	
			try
			{
				fDbPool.setDriverClass ( config.optString ( "driver", "com.mysql.cj.jdbc.Driver" ) );
				fDbPool.setJdbcUrl ( fUrl );
				if ( fDbUser != null )
				{
					fDbPool.setUser ( fDbUser );
				}
				if ( fDbPwd != null )
				{
					fDbPool.setPassword ( fDbPwd );
				}
	
				fDbPool.setMinPoolSize ( config.optInt ( "minPoolSize", 1 ) );
				fDbPool.setAcquireIncrement ( config.optInt ( "acquireIncrement", 1 ) );
				fDbPool.setMaxPoolSize ( config.optInt ( "maxPoolSize", 16 ) );
				fDbPool.setTestConnectionOnCheckout ( true );
			}
			catch ( java.beans.PropertyVetoException e )
			{
				throw new RuntimeException ( e );
			}
		}
		catch ( JSONException x )
		{
			throw new BuildFailure ( x );
		}
	}

	public Connection getConnection () throws SQLException
	{
		return fDbPool.getConnection ();
	}

	private final String fUrl;
	private final String fDbUser;
	private final String fDbPwd;

	private ComboPooledDataSource fDbPool;

	private static final Logger log = LoggerFactory.getLogger ( DbConnection.class );
}