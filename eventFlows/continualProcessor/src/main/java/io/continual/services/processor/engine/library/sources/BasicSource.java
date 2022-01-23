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
import io.continual.services.processor.engine.model.Program;
import io.continual.services.processor.engine.model.Source;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.time.Clock;

public abstract class BasicSource implements Source
{
	@Override
	public synchronized boolean isEof ()
	{
		return fRequeued.size () == 0 && fEof;
	}

	@Override
	public synchronized void close () throws IOException
	{
		noteEndOfStream ();
	}

	@Override
	public synchronized void requeue ( MessageAndRouting msgAndRoute )
	{
		fRequeued.add ( msgAndRoute );
	}

	/**
	 * This basic implementation polls the internalGetNextMessage call periodically until it returns a message, or 
	 * the operation time limit is reached. It also pulls from the requeue list with priority.
	 */
	@Override
	public final MessageAndRouting getNextMessage ( StreamProcessingContext spc, long timeUnit, TimeUnit units ) throws IOException, InterruptedException
	{
		final long[] backoff = getBackoffTimes ();
		int backoffIndex = 0;

		final long endByMs = Clock.now () + TimeUnit.MILLISECONDS.convert ( timeUnit, units );
		do
		{
			synchronized ( this )
			{
				// first check the buffer
				if ( fRequeued.size () > 0 ) return fRequeued.remove ( 0 );

				// is the source stream EOF?
				if ( fEof ) return null;

				// go to the stream
				final MessageAndRouting mr = internalGetNextMessage ( spc );
				if ( mr != null ) return mr;
			}

			final long remainingMs = Math.max ( 0, endByMs - Clock.now () );
			if ( remainingMs > 0 )
			{
				final long backoffTimeMs = Math.min ( remainingMs, backoff [ backoffIndex++ ] );
				if ( backoffIndex == backoff.length ) backoffIndex = 0;	// wrap
	
				log.debug ( "... backing off {} ms", backoffTimeMs );
				Thread.sleep ( backoffTimeMs );	// FIXME could wind up substantially over time
			}
		}
		while ( Clock.now () < endByMs );

		return null;
	}

	@Override
	public synchronized void markComplete ( StreamProcessingContext spc, MessageAndRouting mr )
	{
		// no-op for most sources
	}

	/**
	 * Get the next pending message, if any. This won't be called after a noteEndOfStream() call.
	 * The caller handles back-off, so it's not necessary to force a sleep during this call. 
	 * The object has the instance synchronization lock during this call.
	 * @param spc
	 * @return the next message, or null
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected abstract MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc ) throws IOException, InterruptedException;

	private static final long[] skStdBackoffTimes = new long[] { 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610, 987 };
	protected long[] getBackoffTimes ()
	{
		return skStdBackoffTimes;
	}
	
	protected BasicSource ( String defaultPipelineName )
	{
		fDefPipeline = defaultPipelineName == null ? Program.kDefaultPipeline : defaultPipelineName;
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

	protected synchronized void noteEndOfStream ()
	{
		fEof = true;
	}

	private final String fDefPipeline;
	private final ArrayList<MessageAndRouting> fRequeued;
	private boolean fEof = false;

	private static final Logger log = LoggerFactory.getLogger ( BasicSource.class );
}
