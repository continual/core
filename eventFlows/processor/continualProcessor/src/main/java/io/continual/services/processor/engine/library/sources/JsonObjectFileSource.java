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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.metricTypes.Timer;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.data.json.CommentedJsonTokener;

/**
 * A JSON object stream source.
 */
public class JsonObjectFileSource extends BasicSource
{
	public JsonObjectFileSource ( final ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		super ( config );

		fFilename = config.getString ( "file" );
		fTail = config.optBoolean ( "tail", false );
		fEof = false;
		fSrc = null;
	}

	public JsonObjectFileSource ( final String filename ) throws BuildFailure
	{
		this ( filename, false );
	}

	public JsonObjectFileSource ( final String filename, boolean tail ) throws BuildFailure
	{
		super ( );

		fFilename = filename;
		fTail = tail;
		fEof = false;
		fSrc = null;
	}

	@Override
	public synchronized boolean isEof ()
	{
		return fEof;
	}

	@Override
	protected synchronized MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc ) throws IOException, InterruptedException
	{
		if ( fEof ) return null;

		if ( fSrc == null )
		{
			final String filename = spc.evalExpression ( fFilename );
			log.info ( "loading {}", filename );
			fSrc = new BufferedReader ( new FileReader ( filename ) );
		}

		final MetricsCatalog metrics = spc.getMetrics ().getSubCatalog ( getName () );

		final String line;
		try ( Timer.Context rltc = metrics.timer ( "readLine" ).time () )
		{
			line = fSrc.readLine ();
		}

		if ( line != null )
		{
			final JSONObject data;
			try ( Timer.Context ptc = metrics.timer ( "jsonParse" ).time () )
			{
				data = new JSONObject ( new CommentedJsonTokener ( line ) );
			}

			final Message msg = Message.adoptJsonAsMessage ( data );
			return makeDefRoutingMessage ( msg );
		}
		else
		{
			fEof = true;
			fSrc.close ();
		}
		return null;
	}

	private final String fFilename;
	private final boolean fTail;
	private BufferedReader fSrc;
	private boolean fEof;

	private static final Logger log = LoggerFactory.getLogger ( JsonObjectFileSource.class );
}
