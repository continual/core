/*
 *	Copyright 2021, Continual.io
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

package io.continual.services.processor.library.influxdb.sinks;

import java.time.Instant;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.influxdb.client.WriteApi;
import com.influxdb.client.WriteOptions;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import io.continual.builder.Builder.BuildFailure;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.MetricsCatalog.PathPopper;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Sink;
import io.continual.services.processor.library.influxdb.common.IdbConnection;
import io.continual.services.processor.library.influxdb.common.IdbConnector;
import io.continual.util.data.HumanReadableHelper;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

public class InfluxDbSink extends IdbConnector implements Sink
{
	private static final String kSetting_BufferSize = "postBuffer";
	private static final int kDefault_BufferSize = 64;

	public InfluxDbSink () throws BuildFailure
	{
		this ( new JSONObject () );
	}

	public InfluxDbSink ( JSONObject config ) throws BuildFailure
	{
		this ( (ConfigLoadContext)null, config );
	}

	public InfluxDbSink ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( dbConnectionFromConfig ( sc, config ), sc, config );
	}

	public InfluxDbSink ( IdbConnection dbsrc, ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		super ( dbsrc, sc, config );
		
		try
		{
			fMeasurementExpr = config.getString ( "measurement" );
			fTimeExpr = config.optString ( "timestamp", "${timestamp}" );
			fDataFields = config.getJSONObject ( "data" );
			fTags = config.optJSONObject ( "tags" );

			fRecordCount = 0;

			final int bufferSize = config.optInt ( kSetting_BufferSize, kDefault_BufferSize );
			fWriteApi = getDb().getDb ().getWriteApi ( WriteOptions.builder ().batchSize ( bufferSize ).build () );
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public synchronized void init ()
	{
	}
	
	@Override
	public synchronized void close ()
	{
		flush ();
		fWriteApi.close ();
		log.warn ( "InfluxDbSink closing; sent " + fRecordCount + " records." );
	}

	@Override
	public synchronized void flush ()
	{
	}

	public synchronized long getRecordsSent ()
	{
		return fRecordCount;
	}

	@Override
	public synchronized void process ( MessageProcessingContext context )
	{
		final MetricsCatalog mc = context.getStreamProcessingContext ().getMetrics ();
		try ( PathPopper pp = mc.push ( "InfluxDbSink" ))
		{
			final Point pt = buildPoint ( context );
			fWriteApi.writePoint ( pt );
			fRecordCount++;

			if ( 0 == fRecordCount % 1000 )
			{
				log.info ( "{}K msgs written", HumanReadableHelper.numberValue ( fRecordCount / 1000 ) );
			}
		}
		catch ( Exception e )
		{
			// FIXME: influxdb client doesn't declare checkable exceptions

			log.warn ( "While executing a transaction: " + e.getMessage (), e );
			context.getStreamProcessingContext ().fail ( e.getMessage () );
		}
	}

	private final String fMeasurementExpr;
	private final String fTimeExpr;
	private final JSONObject fDataFields;
	private final JSONObject fTags;
	private long fRecordCount;
	private final WriteApi fWriteApi;

	private static final Logger log = LoggerFactory.getLogger ( InfluxDbSink.class );

	private Point buildPoint ( MessageProcessingContext context ) throws JSONException, NumberFormatException
	{
		final long timeMs = context.evalExpression ( fTimeExpr, Long.class );

		Point p = Point
			.measurement ( context.evalExpression ( fMeasurementExpr ) )
			.time ( Instant.ofEpochMilli ( timeMs ), WritePrecision.MS )
		;

		JsonVisitor.forEachElement ( fDataFields, new ObjectVisitor<JSONObject,NumberFormatException> ()
		{
			@Override
			public boolean visit ( String key, JSONObject entry ) throws JSONException, NumberFormatException
			{
				final String field = context.evalExpression ( key );

				final String expr = entry.getString ( "expr" );
				final String type = entry.optString ( "type", null );

				final String valStr = context.evalExpression ( expr );
				if ( type != null )
				{
					if ( type.equalsIgnoreCase ( "long" ) )
					{
						p.addField ( field, Long.parseLong ( valStr ) );
					}
					else if ( type.equalsIgnoreCase ( "double" ) )
					{
						p.addField ( field, Double.parseDouble ( valStr ) );
					}
					else if ( type.equalsIgnoreCase ( "string" ) )
					{
						p.addField ( field, valStr );
					}
				}
				else
				{
					p.addField ( field, valStr );
				}

				return true;
			}
		} );

		JsonVisitor.forEachElement ( fTags, new ObjectVisitor<String,JSONException> ()
		{
			@Override
			public boolean visit ( String key, String t ) throws JSONException
			{
				p.addTag ( key, context.evalExpression ( t, String.class ));
				return true;
			}
		} );

		return p;
	}
}
