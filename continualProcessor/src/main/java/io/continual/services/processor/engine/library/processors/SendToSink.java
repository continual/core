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

package io.continual.services.processor.engine.library.processors;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;
import io.continual.services.processor.engine.model.Sink;

public class SendToSink implements Processor
{
	public SendToSink ( String sinkName )
	{
		fToSink = sinkName;
		fSink = null;
		fSinkLookupComplete = false;
	}

	public SendToSink ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		try
		{
			fToSink = config.getString ( "to" );
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}

		fSink = null;
		fSinkLookupComplete = false;
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		if ( fSink == null && !fSinkLookupComplete )
		{
			fSinkLookupComplete = true;

			fSink = context.getSink ( fToSink );
			if ( fSink == null )
			{
				context.warn ( "Unknown sink " + fToSink );
			}
		}

		if ( fSink != null )
		{
			fSink.process ( context );
		}
	}

	private final String fToSink;

	private Sink fSink;
	private boolean fSinkLookupComplete;
}
