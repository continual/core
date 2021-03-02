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
import java.util.ArrayList;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;

/**
 * A JSON object stream source.
 */
public class JsonObjectStreamSource extends BasicSource
{
	public JsonObjectStreamSource ( final ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		super ( config );

		fPending = new ArrayList<JSONObject> ();
		fSkip = config.optInt ( "skip", 0 );
	}

	/**
	 * Submit a JSON object for processing through this source.
	 * @param msg
	 */
	public synchronized void submit ( JSONObject msg )
	{
		if ( !isEof () )
		{
			// skip records on the add (to keep EOF checks simple)
			if ( fSkip > 0 )
			{
				fSkip--;
				return;
			}

			fPending.add ( msg );
			notify ();
		}
		else
		{
			throw new IllegalStateException ( "Added JSON msg after close." );
		}
	}
	
	@Override
	public synchronized boolean isEof ()
	{
		return fPending.size () == 0 && super.isEof ();
	}

	@Override
	protected synchronized MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc ) throws IOException, InterruptedException
	{
		if ( fPending.size () > 0 )
		{
			return makeDefRoutingMessage ( Message.adoptJsonAsMessage ( fPending.remove ( 0 ) ) );
		}
		return null;
	}

	private final ArrayList<JSONObject> fPending;
	private int fSkip;
}
