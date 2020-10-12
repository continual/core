package io.continual.services.processor.library.jdbcio;

import java.sql.Connection;
import java.sql.SQLException;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

/**
 * A service wrapper over the db connection
 */
public class DbService extends SimpleService
{
	public DbService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fDb = new DbConnection ( config );
	}

	public DbConnection getConnectionWrapper ()
	{
		return fDb;
	}
	
	public Connection getConnection () throws SQLException
	{
		return getConnectionWrapper().getConnection ();
	}

	private final DbConnection fDb;
}
