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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
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

		fEof = false;

		try
		{
			final String filename = config.getString ( "file" );
			fSrc = new BufferedReader ( new FileReader ( filename ) );
		}
		catch ( JSONException | FileNotFoundException e )
		{
			throw new BuildFailure ( e );
		}
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

		final String line = fSrc.readLine ();
		if ( line != null )
		{
			return makeDefRoutingMessage ( new Message ( new JSONObject ( new CommentedJsonTokener ( line ) ) ) );
		}
		else
		{
			fEof = true;
			fSrc.close ();
		}
		return null;
	}

	private final BufferedReader fSrc;
	private boolean fEof;
}
