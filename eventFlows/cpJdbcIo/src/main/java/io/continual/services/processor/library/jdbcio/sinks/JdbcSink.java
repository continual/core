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

package io.continual.services.processor.library.jdbcio.sinks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Sink;
import io.continual.services.processor.library.jdbcio.DbConnection;
import io.continual.services.processor.library.jdbcio.common.DbConnector;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayOfObjectVisitor;

public class JdbcSink extends DbConnector implements Sink
{
	private static final String kSetting_BufferSize = "postBuffer";
	private static final int kDefault_BufferSize = 64;

	public JdbcSink () throws BuildFailure
	{
		this ( new JSONObject () );
	}

	public JdbcSink ( JSONObject config ) throws BuildFailure
	{
		this ( (ConfigLoadContext)null, config );
	}

	public JdbcSink ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( dbConnectionFromConfig ( sc, config ), sc, config );
	}

	public JdbcSink ( DbConnection dbsrc, ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		super ( dbsrc, sc, config );
		
		try
		{
			// The columns are expressed as an array of objects to preserve order.
			// The objects contain the column header, the evaluation expression, and 
			// any additional metadata like target type.

			fCols = new ArrayList<> ();
			JsonVisitor.forEachObjectIn ( config.optJSONArray ( "columns" ), new ArrayOfObjectVisitor () {
				@Override
				public boolean visit ( JSONObject col ) throws JSONException
				{
					fCols.add ( new ColInfo ( col ) );
					return true;
				}
			} );

			fRecordCount = 0;
			fPendingCount = 0;
			fBufferSize = config.optInt ( kSetting_BufferSize, kDefault_BufferSize );
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	public JdbcSink addColumn ( String key, String value )
	{
		return addColumn ( key, value, String.class );
	}

	/**
	 * Clazz is used to map the value extracted from the message to a prepared statement
	 * setXXX call.  If you use java.util.Date, the system expects a millisecond value.
	 * @param key
	 * @param value
	 * @param clazz
	 * @return
	 */
	public JdbcSink addColumn ( String key, String value, Class<?> clazz )
	{
		return addColumn ( key, value, clazz, null );
	}

	public JdbcSink addColumn ( String key, String value, Class<?> clazz, String fmt )
	{
		fCols.add ( new ColInfo ( key, value, clazz, fmt, 1.0 ) );
		return this;
	}

	@Override
	public synchronized void init ()
	{
		final StringBuilder sb = new StringBuilder ()
			.append ( "REPLACE INTO " )
			.append ( getTable() )	// FIXME: escape this
			.append ( " (" )
		;
		final StringBuilder valPart = new StringBuilder ();
		boolean firstCol = true;
		for ( ColInfo col : fCols )
		{
			if ( !firstCol )
			{
				sb.append ( "," );
				valPart.append ( "," );
			}
			firstCol = false;

			sb.append ( col.getKey () );
			valPart.append ( "?" );
		}
		sb
			.append ( ") VALUES (" )
			.append ( valPart.toString () )
			.append ( ")" )
		;

		fInsertStmt = sb.toString ();
	}

	@Override
	public synchronized void close ()
	{
		flush ();
		log.warn ( "JdbcSink closing; sent " + fRecordCount + " records." );
	}

	@Override
	public synchronized void flush ()
	{
		try
		{
			sendToDb ();
		}
		catch ( SQLException e )
		{
			log.warn ( e.getMessage () );
		}
	}

	public synchronized long getRecordsSent ()
	{
		return fRecordCount;
	}

	@Override
	public synchronized void process ( MessageProcessingContext context )
	{
		try
		{
			if ( fCurrentConnection == null )
			{
				fCurrentConnection = getDb().getConnection ();
			}

			if ( fPending == null )
			{
				fPending = fCurrentConnection.prepareStatement ( fInsertStmt );
			}

			final LinkedList<String> rec = new LinkedList<>();
			
			int param = 1;
			for ( ColInfo ci : fCols )
			{
				final String val = context.evalExpression ( ci.getExpr () );
				final Class<?> targetClass = ci.getTargetClass ();

				rec.add ( ci.getKey() + ": " + val + ( targetClass == String.class ? "" : " (" + targetClass.getName () + ")" ) );

				try
				{
					if ( targetClass.equals ( Integer.class ) )
					{
						final int valToUse;
						if ( val == null || val.length () == 0 )
						{
							valToUse = 0;
						}
						else
						{
							valToUse = Integer.parseInt ( val );
						}
						fPending.setInt ( param, valToUse );
					}
					else if ( targetClass.equals ( Long.class ) )
					{
						fPending.setLong ( param, Long.parseLong ( val ) );
					}
					else if ( targetClass.equals ( Double.class ) )
					{
						double valToUse = 0.0;
						if ( val.trim ().length () == 0 )
						{
							// huh? (it seems to happen...)
							valToUse = 0.0;
						}
						else
						{
							double d = Double.parseDouble ( val );
							final double r = ci.getRounding();
							if ( r != 1.0 )
							{
								d = ( Math.round ( d * r ) ) / r;
							}
							if ( !Double.isFinite ( d ) )
							{
								valToUse = -1.0;
							}
							else
							{
								valToUse = d;
							}
						}
						fPending.setDouble ( param, valToUse );
						rec.add ( "(" + param + ": " + valToUse + ")" );
					}
					else if ( targetClass.equals ( Boolean.class ) )
					{
						fPending.setString ( param, TypeConvertor.convertToBooleanBroad ( val ) ? "Y":"N" );
					}
					else if ( targetClass.equals ( Date.class ) )
					{
						java.sql.Date d = null;
						final String fmt = ci.getFormat ();
						if ( fmt.equals ( "#" ) )
						{
							try
							{
								final long dateval = Long.parseLong ( val );
								d = new java.sql.Date ( dateval ); 
							}
							catch ( NumberFormatException e )
							{
								// ignore
							}
						}
						else
						{
							try
							{
								final SimpleDateFormat sdf = new SimpleDateFormat ( fmt );
								final Date dd = sdf.parse ( val );
								d = new java.sql.Date ( dd.getTime () );
							}
							catch ( ParseException x )
							{
								// ignore
							}
						}

						fPending.setDate ( param, d );
					}
					else if ( targetClass.equals ( Timestamp.class ) )
					{
						fPending.setTimestamp ( param, new Timestamp ( Long.parseLong ( val ) ) );
					}
					else if ( val != null )
					{
						fPending.setString ( param, val );
					}
				}
				catch ( NumberFormatException e )
				{
					fPending.setString ( param, val );
				}
				param++;
			}

			log.debug ( "batch record " + fPendingCount + ": " + rec.toString () );

			fPending.addBatch ();
			fPendingCount++;

			if ( fPendingCount % fBufferSize == 0 )
			{
				sendToDb ();
			}
		}
		catch ( SQLException e )
		{
			log.warn ( "While executing a transaction, a SQL Exception: " + e.getMessage () );
			if ( fCurrentConnection != null )
			{
				try
				{
					fCurrentConnection.close ();	// FIXME: this doesn't cause the pool to dismiss the connection?
				}
				catch ( SQLException e1 )
				{
					log.warn ( "While closing a connection (during an exception), a SQL Exception: " + e.getMessage () );
				}
				fCurrentConnection = null;
			}
			context.getStreamProcessingContext ().fail ( e.getMessage () );
		}
	}

	private String fInsertStmt;
	private final ArrayList<ColInfo> fCols;

	private long fPendingCount;
	private long fRecordCount;
	private final int fBufferSize;

	private Connection fCurrentConnection;
	private PreparedStatement fPending;

	private static final Logger log = LoggerFactory.getLogger ( JdbcSink.class );

	private void sendToDb () throws SQLException
	{
		if ( fPending != null )
		{
			try
			{
				fPending.executeBatch ();
				fPending.close ();
				fPending = null;
			}
			catch ( SQLException x )
			{
				log.warn ( x.getMessage (), x );
				if ( fCurrentConnection != null )
				{
					fCurrentConnection.close ();
					fCurrentConnection = null;
				}
				throw x;
			}
		}

		fRecordCount += fPendingCount;
		log.info ( "Posted {} records, total {}.", fPendingCount, fRecordCount );
		fPendingCount = 0;
	}
	
	private static class ColInfo
	{
		public ColInfo ( String key, String expr, Class<?> clazz, String fmt, double rounding )
		{
			fKey = key;
			fExpr = expr;
			fClass = clazz;
			fFormat = fmt;
			fRounding = rounding;
		}

		public ColInfo ( JSONObject data )
		{
			this (
				data.getString ( "key" ),
				data.optString ( "expr", "${" + data.getString ( "key" ) + "}" ),
				classFrom ( data.optString ( "type", null ) ),
				data.optString ( "format", null ),
				data.optDouble ( "rounding", 1.0 )
			);
		}

		@Override
		public String toString ()
		{
			return new StringBuilder ()
				.append ( fKey )
				.append ( " from " )
				.append ( fExpr )
				.append ( " as " )
				.append ( fClass.getSimpleName () )
				.toString ()
			;
		}
		
		public String getKey () { return fKey; }
		public String getExpr () { return fExpr; }
		public Class<?> getTargetClass () { return fClass; }
		public String getFormat() { return fFormat; }
		public double getRounding () { return fRounding; }

		private final String fKey;
		private final String fExpr;
		private final Class<?> fClass;
		private final String fFormat;
		private final double fRounding;

		private static Class<?> classFrom ( String text )
		{
			if ( text == null || text.length () == 0 ) return String.class;

			text = text.trim ().toLowerCase ();
			if ( text.startsWith ( "int" ) )
			{
				return Integer.class;
			}
			else if ( text.startsWith ( "long" ) )
			{
				return Long.class;
			}
			else if ( text.startsWith ( "double" ) )
			{
				return Double.class;
			}
			else if ( text.startsWith ( "bool" ) )
			{
				return Boolean.class;
			}
			else if ( text.startsWith ( "date" ) )
			{
				return Date.class;
			}
			else if ( text.startsWith ( "timestamp" ) )
			{
				return Timestamp.class;
			}
			// etc....

			return String.class;
		}
	}
}
