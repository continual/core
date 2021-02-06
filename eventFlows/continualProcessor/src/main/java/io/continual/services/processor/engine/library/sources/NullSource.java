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
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.Source;
import io.continual.services.processor.engine.model.StreamProcessingContext;

/**
 * A CSV stream source. This source will report EOF when all records are read.
 */
public class NullSource implements Source
{
	public NullSource ( JSONObject config ) throws BuildFailure
	{
		this ( null, config );
	}

	public NullSource ( final ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
	}

	@Override
	public void close () throws IOException
	{
	}

	@Override
	public boolean isEof ()
	{
		return true;
	}

	@Override
	public MessageAndRouting getNextMessage ( StreamProcessingContext spc, long timeUnit, TimeUnit units ) throws InterruptedException
	{
		// always sleep, then return null
		Thread.sleep ( units.convert ( timeUnit, TimeUnit.MILLISECONDS ) );
		return null;
	}

	@Override
	public void requeue ( MessageAndRouting msg )
	{
		// nothing to do here
	}

	@Override
	public void markComplete ( StreamProcessingContext spc, MessageAndRouting mr )
	{
		// no-op
	}
}
