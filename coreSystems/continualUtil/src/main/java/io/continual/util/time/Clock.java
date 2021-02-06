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

package io.continual.util.time;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic clock service, replaces System.currentTimeMillis(), but with test access.
 */
public class Clock
{
	/**
	 * Return the current time in milliseconds
	 * @return the time in milliseconds
	 */
	public static long now ()
	{
		return holder.instance.nowMs ();
	}

	/**
	 * Provided for testing only. 
	 * @param c the replacement clock
	 */
	public static void replaceClock ( Clock c )
	{
		holder.instance = c;
	}

	/**
	 * Switch to a test clock and return that instance. Equivalent to instantiating
	 * a clock.testClock and calling replaceClock() with it.
	 * 
	 * @return a test clock.
	 */
	public static TestClock useNewTestClock ()
	{
		final TestClock tc = new TestClock ();
		replaceClock ( tc );
		return tc;
	}

	protected long nowMs ()
	{
		// the usual system clock
		return System.currentTimeMillis ();
	}

	private static class holder
	{
		// volatile: harmless in normal runs, as this is a singleton constructed
		// once and shared among threads (all cache the same reference). For test
		// runs (e.g. from JUnit), it ensures that replaceClock() takes effect in
		// all threads immediately.
		static volatile Clock instance =
			( null == System.getProperty ( "timeStart" ) && null == System.getProperty ( "timeScale" ) ) ?
			new Clock () :
			new ScaledClock ();
	}

	/**
	 * A simple testing clock.
	 */
	public static class TestClock extends Clock
	{
		@Override
		public long nowMs () { return nowMs; }

		public TestClock set ( long ms ) { nowMs = ms; return this; }
		public TestClock add ( long ms ) { nowMs += ms; return this; }
		public TestClock add ( long val, TimeUnit tu )
		{
			return add ( TimeUnit.MILLISECONDS.convert ( val, tu ) );
		}

		private long nowMs = 1;
	}

	/**
	 * A clock that starts at a given time and passes time at a given scale.
	 */
	public static class ScaledClock extends Clock
	{
		public ScaledClock ()
		{
			// for example, use -DtimeStart=-604800000 -DtimeScale=900 to setup the clock
			// to begin one week ago and have every second count as 15 minutes.

			final String timeStart = System.getProperty ( "timeStart" );
			final String timeScale = System.getProperty ( "timeScale" );

			long startMs = System.currentTimeMillis ();
			if ( timeStart != null )
			{
				try
				{
					startMs = Long.parseLong ( timeStart );
					if ( startMs < 0 )
					{
						// offset from now
						startMs = System.currentTimeMillis () + startMs;
					}
				}
				catch ( NumberFormatException x )
				{
					log.warn ( "Couldn't parse timeStart: " + x.getMessage () );
					startMs = System.currentTimeMillis ();
				}
			}

			double scale = 1.0;
			if ( timeScale != null )
			{
				try
				{
					scale = Double.parseDouble ( timeScale );
					if ( scale <= 0 )
					{
						log.warn ( "Time scale must be a positive number." );
						scale = 1.0;
					}
				}
				catch ( NumberFormatException x )
				{
					log.warn ( "Couldn't parse timeScale: " + x.getMessage () );
					scale = 1.0;
				}
			}

			fStartMs = startMs;
			fScale = scale;
		}

		@Override
		public long nowMs ()
		{
			return fStartMs + Math.round ( ( super.nowMs () - fStartMs ) * fScale );
		}

		private final long fStartMs;
		private final double fScale;
	}

	private static final Logger log = LoggerFactory.getLogger ( Clock.class );
}
