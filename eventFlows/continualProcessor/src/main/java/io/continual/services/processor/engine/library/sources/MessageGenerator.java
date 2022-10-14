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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.util.time.Clock;

/**
 * A message generating source
 */
public class MessageGenerator extends QueuingSource
{
	public MessageGenerator ( JSONObject config ) throws BuildFailure
	{
		this ( null, config );
	}

	public MessageGenerator ( final ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		super ( config );

		try
		{
			final JSONObject given = config.optJSONObject ( "message" );
			fMessage = given == null ? new JSONObject () : given;
			fPauseMs = config.optLong ( "everyMs", 1000 );
	
			fNextMs = Clock.now () + fPauseMs;
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	protected List<MessageAndRouting> reload ()
	{
		final ArrayList<MessageAndRouting> result = new ArrayList<> ();
		if ( Clock.now () >= fNextMs )
		{
			fNextMs += fPauseMs;
			result.add ( makeDefRoutingMessage ( Message.copyJsonToMessage ( fMessage.put ( "serialNumber", ++fSerialNumber ) ) ) );
		}

		// if we're not generating at a frequency, that was the only message.
		if ( fPauseMs == 0 )
		{
			noteEndOfStream ();
			fNextMs = Long.MAX_VALUE;
		}
		return result;
	}

	private final JSONObject fMessage;
	private final long fPauseMs;

	private long fNextMs;
	private long fSerialNumber = 0;
}
