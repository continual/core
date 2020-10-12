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
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.Source;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.time.Clock;

public abstract class BasicSource implements Source
{
	@Override
	public boolean isEof ()
	{
		return fRequeued.size () == 0 && fEof;
	}

	@Override
	public void close () throws IOException
	{
		fEof = true;
	}

	@Override
	public synchronized void requeue ( MessageAndRouting msgAndRoute )
	{
		fRequeued.add ( msgAndRoute );
	}

	@Override
	public synchronized final MessageAndRouting getNextMessage ( StreamProcessingContext spc, long timeUnit, TimeUnit units ) throws IOException, InterruptedException
	{
		if ( fRequeued.size () > 0 )
		{
			return fRequeued.remove ( 0 );
		}

		final long endBy = Clock.now () + TimeUnit.MILLISECONDS.convert ( timeUnit, units );
		final long[] backoff = getBackoffTimes ();
		int backoffIndex = 0;
		do
		{
			final MessageAndRouting mr = internalGetNextMessage ( spc, timeUnit, units );
			if ( mr != null ) return mr;

			final long backoffTimeMs = backoff[backoffIndex];
			log.debug ( "... backing off {} ms", backoffTimeMs );
			Thread.sleep ( backoffTimeMs );	// FIXME could wind up substantially over time
			backoffIndex = Math.min ( backoffIndex+1, backoff.length-1 );
		}
		while ( Clock.now () < endBy );

		return null;
	}

	@Override
	public void markComplete ( StreamProcessingContext spc, MessageAndRouting mr )
	{
		// no-op for most sources
	}

	protected abstract MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc, long timeUnit, TimeUnit units ) throws IOException, InterruptedException;

	protected long[] getBackoffTimes ()
	{
		return new long[] { 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987 };
	}
	
	protected BasicSource ( String defaultPipelineName )
	{
		fDefPipeline = defaultPipelineName;
		fRequeued = new ArrayList<> ();
	}

	protected BasicSource ( JSONObject config )
	{
		this ( config.getString ( "pipeline" ) );
	}

	protected BasicSource ( )
	{
		this ( (String) null );
	}

	protected MessageAndRouting makeDefRoutingMessage ( final Message msg )
	{
		return new MessageAndRouting ( msg, fDefPipeline );
	}

	protected void noteEndOfStream ()
	{
		fEof = true;
	}

	private final String fDefPipeline;
	private final ArrayList<MessageAndRouting> fRequeued;
	private boolean fEof = false;

	private static final Logger log = LoggerFactory.getLogger ( BasicSource.class );
}
