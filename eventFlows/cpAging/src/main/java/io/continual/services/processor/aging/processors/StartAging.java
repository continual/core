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

package io.continual.services.processor.aging.processors;

import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.StreamProcessingContext.NoSuitableObjectException;

public class StartAging extends BaseAgingProcessor
{
	public StartAging ( ConfigLoadContext sc, JSONObject config )
	{
		super ( sc, config );

		fAgeMs = config.getLong ( "agingMs" );
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		try
		{
			getService ( context ).startAging ( context, fAgeMs, TimeUnit.MILLISECONDS );
		}
		catch ( NoSuitableObjectException e )
		{
			context.stopProcessing ( e.getMessage () );
		}
	}

	private final long fAgeMs;
}
