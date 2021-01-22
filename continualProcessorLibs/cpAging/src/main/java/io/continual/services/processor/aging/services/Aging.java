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

package io.continual.services.processor.aging.services;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.service.SimpleProcessingService;
import io.continual.util.time.Clock;

public class Aging extends SimpleProcessingService
{
	public Aging ( ConfigLoadContext sc, JSONObject config )
	{
		fPending = new DelayQueue<> ();
		fThread = new ServiceThread ();

		fOnComplete = config.optString ( "onComplete", null );
		fOnCancel = config.optString ( "onCancel", null );
	}

	public void startAging ( MessageProcessingContext mpc, long lengthOfTime, TimeUnit timeUnits )
	{
		fPending.add (
			new MessageContainer ( mpc, Clock.now () + timeUnits.convert ( lengthOfTime, TimeUnit.MILLISECONDS ) )
		);
	}

	public void cancelAging ( Message message )
	{
	}

	private final DelayQueue<MessageContainer> fPending;
	private final ServiceThread fThread;
	private final String fOnComplete;
	private final String fOnCancel;

	@Override
	public boolean isRunning ()
	{
		return fThread.isAlive ();
	}

	@Override
	protected void onStart ()
	{
		fThread.start ();
	}

	@Override
	protected void onStopRequested ()
	{
		fThread.requestStop ();
	}

	private static class MessageContainer implements Delayed
	{
		public MessageContainer ( MessageProcessingContext msg, long expiresAtMs )
		{
			fMsg = msg;
			fExpiresAtMs = expiresAtMs;
		}

		@Override
		public int compareTo ( Delayed o )
		{
			return Long.compare (
				getDelay ( TimeUnit.MILLISECONDS ),
				o.getDelay ( TimeUnit.MILLISECONDS )
			);
		}

		@Override
		public long getDelay ( TimeUnit unit )
		{
			return unit.convert ( fExpiresAtMs - Clock.now (), TimeUnit.MILLISECONDS );
		}

		public MessageProcessingContext getMessageProcessingContext () { return fMsg; }

		private final MessageProcessingContext fMsg;
		private final long fExpiresAtMs;
	}

	private class ServiceThread extends Thread
	{
		public ServiceThread ()
		{
			fShouldRun = new AtomicBoolean ( true );
		}

		public void requestStop ()
		{
			fShouldRun.set ( false );
		}

		@Override
		public void run ()
		{
			try
			{
				while ( fShouldRun.get () )
				{
					final MessageContainer mc = fPending.poll ( 500, TimeUnit.MILLISECONDS );
					if ( mc != null && fOnComplete != null )
					{
						// requeue
						final MessageProcessingContext mpc = mc.getMessageProcessingContext ();
						mpc.getStreamProcessingContext ().requeue ( new MessageAndRouting ( mpc.getMessage (), fOnComplete ) );
					}
				}
			}
			catch ( InterruptedException e )
			{
				log.warn ( "Aging thread interrupted." );
			}
		}

		private final AtomicBoolean fShouldRun;
	}

	private static final Logger log = LoggerFactory.getLogger ( Aging.class );
}
