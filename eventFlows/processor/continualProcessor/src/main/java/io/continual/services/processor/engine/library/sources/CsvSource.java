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

package io.continual.services.processor.engine.library.sources;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.resources.ResourceLoader;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

/**
 * A CSV stream source. This source will report EOF when all records are read.
 */
public class CsvSource extends BasicSource
{
	public CsvSource ( JSONObject config ) throws BuildFailure
	{
		this ( null, config );
	}

	public CsvSource ( String defPipeline, String resource ) throws BuildFailure
	{
		super ( defPipeline );
		try
		{
			fResource = resource;
			fFieldDelim = null;
			fFieldList = null;

			fLineNumberToField = null;

			fFieldMap = new HashMap<> ();
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	public CsvSource ( final ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		super ( config );
		try
		{
			fResource = config.optString ( "data", "" );
			fFieldDelim = config.optString ( "fieldDelimiter", null );
			fFieldList = JsonVisitor.arrayToList ( config.optJSONArray ( "fields" ) );

			fLineNumberToField = config.optString ( "lineNumberTo", null );

			fFieldMap = new HashMap<> ();
			JsonVisitor.forEachElement ( config.optJSONObject ( "fieldMap" ), new ObjectVisitor<String,JSONException> ()
			{
				@Override
				public boolean visit ( String srcField, String mappedField ) throws JSONException
				{
					fFieldMap.put ( srcField, mappedField );
					return true;
				}
			} );
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public void close () throws IOException
	{
		if ( fParser != null ) fParser.close ();
		fParser = null;

		fInited = true;	// not really, but we want isEof() 

		super.close ();
	}

	/**
	 * Provided to connect an existing input stream to this CSV parser source. Overrides the resource
	 * specified to the constructor.
	 * @param data
	 */
	public CsvSource setResource ( InputStream data )
	{
		fStream = data;
		return this;
	}

	@Override
	public boolean isEof ()
	{
		return fInited && fParser == null;
	}

	@Override
	protected MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc ) throws IOException
	{
		// normal processing...
		init ( spc );

		// maybe it was closed?
		if ( isEof() ) return null;

		// get the next record
		if ( fIterator.hasNext () )
		{
			fLineNumber++;
			return buildMessage ( fIterator.next () );
		}

		// out of records, close the iterator/parser as an EOF signal
		fParser.close ();
		fParser = null;

		return null;
	}

	private final String fResource;
	private final String fFieldDelim;
	private final List<String> fFieldList;
	private final String fLineNumberToField;
	private final HashMap<String,String> fFieldMap;

	private boolean fInited = false;
	private InputStream fStream = null;
	private CSVParser fParser = null;
	private Iterator<CSVRecord> fIterator = null;
	private int fLineNumber;

	private void init ( StreamProcessingContext spc ) throws IOException
	{
		if ( !fInited )
		{
			fInited = true;

			// locate the stream if not provided
			if ( fStream == null )
			{
				final String name = spc.evalExpression ( fResource );
				if ( name == null || name.length () == 0 )
				{
					throw new IOException ( "No resource name provided." );
				}
				fStream = ResourceLoader.load ( name );
				log.info ( "loaded {}", name );
			}
			if ( fStream == null )
			{
				final String name = spc.evalExpression ( fResource );
				throw new IOException ( "Unable to load resource: " + name + " (" + fResource + ")" );
			}

			// put together the reader format
			CSVFormat format = CSVFormat.DEFAULT;
			if ( fFieldDelim != null )
			{
				// literal tab or escaped tab syntax
				if ( fFieldDelim.equals ( "\\t" ) || fFieldDelim.equals ( "\t" ) )
				{
					format = CSVFormat.TDF;
				}
			}
			if ( fFieldList != null && fFieldList.size () > 0 )
			{
				format = format.withHeader ( fFieldList.toArray ( new String[ fFieldList.size () ] ) );
			}
			else
			{
				format = format.withFirstRecordAsHeader ();
			}
			
			// create the parser
			fParser = new CSVParser ( new InputStreamReader ( fStream ), format );
			fIterator = fParser.iterator ();

			fLineNumber = 1;	// for the header
		}
	}

	private MessageAndRouting buildMessage ( CSVRecord record )
	{
		final JSONObject data = new JSONObject ();
		for ( String key : fParser.getHeaderMap ().keySet () )
		{
			final String newKey = fFieldMap.get ( key );
			data.put ( newKey != null ? newKey : key, record.get ( key ) );
		}
		if ( fLineNumberToField != null )
		{
			data.put ( fLineNumberToField, fLineNumber );
		}
		return makeDefRoutingMessage ( Message.adoptJsonAsMessage ( data ) );
	}

	private static final Logger log = LoggerFactory.getLogger ( CsvSource.class );
}
