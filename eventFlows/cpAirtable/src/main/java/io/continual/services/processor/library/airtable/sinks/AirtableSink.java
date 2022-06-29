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
import java.util.HashMap;
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
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

public class AirtableSink implements Sink
{
	public enum FieldWriteStrategy
	{
		// always overwrite
		OVERWRITE,

		// overwrite the value if the target is empty
		OVERWRITE_IF_TARGET_IS_EMPTY,

		// overwrite the target value if the source has a value
		OVERWRITE_IF_SOURCE_HAS_VALUE,

		// append the source value to a target value
		APPEND
	}
	
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

			fFieldWriteStrategy = new HashMap<> ();
			final JSONObject wsConfig = config.optJSONObject ( "writeStrategy" );
			JsonVisitor.forEachElement ( wsConfig, new ObjectVisitor<String,BuildFailure> ()
			{
				@Override
				public boolean visit ( String key, String val ) throws JSONException, BuildFailure
				{
					try
					{
						final FieldWriteStrategy fws = FieldWriteStrategy.valueOf ( val.toUpperCase () );
						fFieldWriteStrategy.put ( key, fws );
					}
					catch ( IllegalArgumentException x )
					{
						throw new BuildFailure ( x );
					}
					return true;
				}
				
			} );

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
		String keyVal = "";

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
			AirtableRecord existingRecord = null;
			if ( fKey != null )
			{
				keyVal = msg.getValueAsString ( fKey );
				if ( keyVal == null ) keyVal = "";

				// search for value in the key field
				final List<AirtableRecord> existing = fAirtable.listRecords ( fAirtable.createSelector ( fTable ).filterWith ( fKey + "='" + keyVal + "'" ) );
				if ( existing.size () > 0 )
				{
					existingRecord = existing.get ( 0 );
					existingRecordId = existingRecord.getId ();
				}
			}

			// update or create a record
			if ( existingRecord != null )
			{
				final JSONObject update = merge ( existingRecord, obj );
				fAirtable.patchRecord ( fTable, existingRecordId, update ); 
			}
			else
			{
				fAirtable.createRecord ( fTable, obj );
			}
			fRecordCount++;
		}
		catch ( IOException | AirtableRequestException | AirtableServiceException e )
		{
			context.warn ( "Unable to post record [" + keyVal + "] to Airtable: " + e.getMessage () );
		}
		finally
		{
		}
	}

	private JSONObject merge ( AirtableRecord existingRecord, JSONObject newData )
	{
		final JSONObject result = new JSONObject ();
		JsonVisitor.forEachElement ( newData, new ObjectVisitor<Object,JSONException> ()
		{
			@Override
			public boolean visit ( String key, Object newVal ) throws JSONException
			{
				FieldWriteStrategy fws = fFieldWriteStrategy.get ( key );
				if ( fws == null )
				{
					fws = FieldWriteStrategy.OVERWRITE_IF_SOURCE_HAS_VALUE;
				}

				final Object existingValue = existingRecord.getRawValue ( key );

				switch ( fws )
				{
					case APPEND:
					{
						// appending is tricky... if source 1 is sending "foo" and source 2 is sending "bar", they'll never have
						// values equal to the stored value and we'd end up appending the values over and over again.  Instead,
						// we'll try a case-insensitive "contains" to decide if we need an update.

						final String existingStr = existingValue == null ? "" : existingValue.toString ().trim ();
						final String newStr = newVal == null ? "" : newVal.toString ().trim();
						if ( !existingStr.toLowerCase ().contains ( newStr.toLowerCase () ) )
						{
							result.put ( key, existingStr + "\n" + newStr );
						}
						else
						{
							result.put ( key, existingStr );
						}
					}
					break;

					case OVERWRITE:
					{
						result.put ( key, newVal );
					}
					break;

					case OVERWRITE_IF_TARGET_IS_EMPTY:
					{
						final String existingStr = existingValue == null ? "" : existingValue.toString ();
						if ( existingStr.length () == 0 )
						{
							result.put ( key, newVal );
						}
					}
					break;

					default:
					case OVERWRITE_IF_SOURCE_HAS_VALUE:
					{
						final String newStr = newVal == null ? "" : newVal.toString ();
						if ( newStr.length () > 0 )
						{
							result.put ( key, newVal );
						}
					}
					break;
				}
				return true;
			}
		} );
		return result;
	}

	private final AirtableClient fAirtable;
	private final String fTable;
	private final String fKey;
	private final String fRecordBlock;
	private long fRecordCount;
	private final HashMap<String,FieldWriteStrategy> fFieldWriteStrategy;

	private static final Logger log = LoggerFactory.getLogger ( AirtableSink.class );
}
