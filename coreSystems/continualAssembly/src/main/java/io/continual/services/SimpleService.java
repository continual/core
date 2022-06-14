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

package io.continual.services;

import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.continual.util.time.Clock;

/**
 * A simple service doesn't start/stop.
 */
public class SimpleService implements Service
{
	public SimpleService ()
	{
	}

	public SimpleService ( ServiceContainer sc, JSONObject config )
	{
	}

	@Override
	public synchronized void start () throws FailedToStart
	{
		fStopped = false;
		onStartRequested ();
	}

	@Override
	public synchronized void requestFinish ()
	{
		fStopped = true;
		onStopRequested ();
	}

	/**
	 * Request this service to finish and then wait for up to the given time. 
	 * @param duration
	 * @param units
	 * @throws InterruptedException
	 */
	public void requestFinishAndWait ( long duration, TimeUnit units ) throws InterruptedException
	{
		requestFinish ();

		final long waitUntilMs = Clock.now () + TimeUnit.MILLISECONDS.convert ( duration, units );
		while ( isRunning () && Clock.now () <= waitUntilMs )
		{
			Thread.sleep ( 100 );
		}
	}

	@Override
	public synchronized boolean isRunning ()
	{
		return !fStopped;
	}

	private boolean fStopped = true;

	protected void onStartRequested () throws FailedToStart {}
	protected void onStopRequested () {}
}
