/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.services.processor.library.jdbcio.sources;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.Source;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.services.processor.library.jdbcio.DbConnection;
import io.continual.services.processor.library.jdbcio.common.DbConnector;

public class JdbcSource extends DbConnector implements Source
{
	public JdbcSource ( JSONObject config ) throws BuildFailure
	{
		this ( (ConfigLoadContext)null, config );
	}

	public JdbcSource ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( dbConnectionFromConfig ( config ), config );
	}

	public JdbcSource ( DbConnection dbsrc, JSONObject config ) throws BuildFailure
	{
		super ( dbsrc, config );

		fQuery = config.getString ( "query" );
		fPipeline = config.getString ( "pipeline" );
	}

	@Override
	public boolean isEof () throws IOException
	{
		return fRows != null && fRows.size () == 0;
	}

	@Override
	public MessageAndRouting getNextMessage ( StreamProcessingContext spc, long waitAtMost, TimeUnit waitAtMostTimeUnits ) throws IOException, InterruptedException
	{
		try
		{
			init ();
			if ( fRows != null && fRows.size () > 0 )
			{
				final JSONObject data = fRows.remove ( 0 );
				return new MessageAndRouting ( new Message ( data ), fPipeline );
			}
		}
		catch ( SQLException x )
		{
			spc.warn ( "Couldn't fetch JDBC records. " + x.getMessage () );
		}
		return null;
	}

	@Override
	public void markComplete ( StreamProcessingContext spc, MessageAndRouting mr )
	{
		// not supported
	}

	@Override
	public void requeue ( MessageAndRouting msgAndRoute )
	{
		// ignore
	}

	public void init () throws SQLException
	{
		if ( fRows != null ) return;

		fRows = new ArrayList<> ();
		try ( final Connection cc = getDb ().getConnection () )
		{
			final PreparedStatement ps = cc.prepareStatement ( fQuery );
			final ResultSet rs = ps.executeQuery ();
			final ResultSetMetaData md = rs.getMetaData ();
			while ( rs.next () )
			{
				final JSONObject msg = new JSONObject ();
				for ( int i=1; i<=md.getColumnCount (); i++ )
				{
					final String colName = md.getColumnName ( i );
					final Object val;
					switch ( md.getColumnType ( i ) )
					{
						case java.sql.Types.INTEGER:
						case java.sql.Types.BIGINT:
						case java.sql.Types.SMALLINT:
						case java.sql.Types.TINYINT:
						{
							val = rs.getInt ( i );
						}
						break;

						case java.sql.Types.FLOAT:
						case java.sql.Types.REAL:
						case java.sql.Types.DOUBLE:
						{
							val = rs.getDouble ( i );
						}
						break;

						// FIXME: do the others...

						default:
						{
							val = rs.getString ( i );
						}
						break;
					}
					msg.put ( colName, val );
				}

				fRows.add ( msg );
			}
		}
	}

	@Override
	public void close ()
	{
		// ignore
	}

	private final String fQuery;
	private final String fPipeline;
	private ArrayList<JSONObject> fRows = null;
}
