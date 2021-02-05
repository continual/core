package io.continual.services.processor.library.jdbcio.processors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;
import io.continual.services.processor.library.jdbcio.DbConnection;
import io.continual.services.processor.library.jdbcio.common.DbConnector;
import io.continual.util.collections.LruCache;
import io.continual.util.data.json.JsonUtil;

public class JdbcLookup extends DbConnector implements Processor
{
	public JdbcLookup () throws BuildFailure
	{
		this ( new JSONObject () );
	}

	public JdbcLookup ( JSONObject config ) throws BuildFailure
	{
		this ( (ConfigLoadContext)null, config );
	}

	public JdbcLookup ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( dbConnectionFromConfig ( sc, config ), sc, config );
	}

	public JdbcLookup ( DbConnection dbsrc, ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		super ( dbsrc, sc, config );

		fLookupField = config.getString ( "lookupField" );
		fLookupValue = config.getString ( "lookupValue" );
		fToField = config.getString ( "toField" );

		fCache = new LruCache<> ( 4096 );
		fCaching = config.optBoolean ( "cache", true );
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		try
		{
			final String lf = context.evalExpression ( fLookupField );
			final String lv = context.evalExpression ( fLookupValue );

			final JSONObject data;
			if ( fCaching && fCache.containsKey ( lv ) )
			{
				data = fCache.get ( lv );
			}
			else
			{
				try ( final Connection c = getDb ().getConnection () )
				{
					final PreparedStatement ps = c.prepareStatement ( "SELECT * FROM " + getTable() + " WHERE " + lf + "=?" );
					ps.setString ( 1, lv );

					final ResultSet rs = ps.executeQuery ();
					if ( rs.next () )
					{
						data = new JSONObject ();
		
						final ResultSetMetaData rsm = rs.getMetaData ();
						for ( int i=1; i<=rsm.getColumnCount (); i++ )
						{
							final String label = rsm.getColumnName ( i );
							data.put ( label, rs.getString ( i ) );
						}
						fCache.put ( lv, data );

						if ( rs.next () )
						{
							context.warn ( "Lookup for " + lf + "=" + lv + " returned more than one result." );
						}
					}
					else
					{
						data = null;
					}
				}
			}

			if ( data != null )
			{
				context.getMessage ().putRawValue ( fToField, JsonUtil.clone ( data ) );
			}
		}
		catch ( SQLException e )
		{
			context.warn ( e.getMessage () );
		}
	}

	private final String fLookupField;
	private final String fLookupValue;
	private final String fToField;

	private final boolean fCaching;
	private final LruCache<String,JSONObject> fCache;
}
