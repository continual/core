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

package io.continual.services.processor.library.airtable.sinks;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rathravane.airtableClient.AirtableClient;
import com.rathravane.airtableClient.AirtableClient.AirtableRequestException;
import com.rathravane.airtableClient.AirtableClient.AirtableServiceException;
import com.rathravane.airtableClient.AirtableClientFactory;
import com.rathravane.airtableClient.AirtableRecord;

import io.continual.builder.Builder.BuildFailure;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.MetricsCatalog.PathPopper;
import io.continual.services.ServiceContainer;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Sink;
import io.continual.util.data.exprEval.ExpressionEvaluator;

public class AirtableSink implements Sink
{
	public AirtableSink ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( sc.getServiceContainer (), config );
	}

	public AirtableSink ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		try
		{
			final ExpressionEvaluator ee = sc.getExprEval ();

			fTable = ee.evaluateText ( config.getString ( "table" ) );
			fKey = ee.evaluateText ( config.optString ( "key", null ) );
			fRecordBlock = ee.evaluateText ( config.optString ( "block", null ) );

			final String base = ee.evaluateText ( config.getString ( "base" ) );
			fAirtable = new AirtableClientFactory ()
				.usingBase ( base )
				.withApiKey ( ee.evaluateText ( config.getString ( "apiKey" ) ) )
				.build ()
			;
			
			log.info ( "Using Airtable base {}.", base );
		}
		catch ( JSONException x )
		{
			throw new BuildFailure ( x );
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
		log.warn ( "AirtableSink closing; sent " + fRecordCount + " records." );
	}

	@Override
	public synchronized void flush ()
	{
		// nothing to do here
	}

	public synchronized long getRecordsSent ()
	{
		return fRecordCount;
	}

	@Override
	public synchronized void process ( MessageProcessingContext context )
	{
		final MetricsCatalog mc = context.getStreamProcessingContext ().getMetrics ();
		try ( PathPopper pp = mc.push ( "AirtableSink" ))
		{
			final Message msg = context.getMessage ();

			// get the json to post
			JSONObject obj = msg.accessRawJson ();
			if ( fRecordBlock != null )
			{
				obj = obj.optJSONObject ( fRecordBlock );
				if ( obj == null )
				{
					log.warn ( "No Airtable sink message block at {}.", fRecordBlock );
					return;
				}
			}

			// is this an overwrite or a create?
			String existingRecordId = null;
			if ( fKey != null )
			{
				String keyVal = msg.getValueAsString ( fKey );
				if ( keyVal == null ) keyVal = "";

				// search for value in the key field
				final List<AirtableRecord> existing = fAirtable.listRecords ( fAirtable.createSelector ( fTable ).filterWith ( fKey + " ='" + keyVal + "'" ) );
				if ( existing.size () > 0 )
				{
					existingRecordId = existing.get ( 0 ).getId ();
				}
			}

			// update or create a record
			if ( existingRecordId != null )
			{
				fAirtable.patchRecord ( fTable, existingRecordId, obj ); 
			}
			else
			{
				fAirtable.createRecord ( fTable, obj );
			}
			fRecordCount++;
		}
		catch ( IOException | AirtableRequestException | AirtableServiceException e )
		{
			context.warn ( "Unable to post record to Airtable: " + e.getMessage () );
		}
		finally
		{
		}
	}

	private final AirtableClient fAirtable;
	private final String fTable;
	private final String fKey;
	private final String fRecordBlock;
	private long fRecordCount;

	private static final Logger log = LoggerFactory.getLogger ( AirtableSink.class );
}
