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

import io.continual.util.data.StringUtils;

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
		// volatile: harmless in normal runs, as this is a singleton constructed once and shared among threads (all
		// cache the same reference). For test runs (e.g. from JUnit), it ensures that replaceClock() takes effect
		// in all threads immediately.

		static volatile Clock instance =
			( StringUtils.isNotEmpty ( System.getProperty ( skTimeStartMs ) ) || StringUtils.isNotEmpty ( System.getProperty ( skTimeScaleArg ) ) ) ?
			new ScaledClock () :
			new Clock ()
		;
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
		/**
		 * Create a scaled clock based on JVM parameters, default to now() as start time
		 * and 1.0 as the scale factor, like a normal clock.
		 */
		public ScaledClock ()
		{
			// for example, use -DcontinualClockStartMs=-604800000 -DcontinualClockScale=900 to setup the clock
			// to begin one week ago and have every second count as 15 minutes.
			this ( getConfiguredStartTimeMs(), getConfiguredTimeScale() );
		}

		/**
		 * Created a scaled clock starting at the given time with the given scale. If the start
		 * time value is less than 0, the value is subtracted from "now" to start the clock.
		 * 
		 * @param startMs the clock's start time, or number of milliseconds before now()
		 * @param scale a scale factor 
		 */
		public ScaledClock ( long startMs, double scale )
		{
			fStartMs = makeStartTimeFrom ( startMs );
			fScale = scale;
			fActualStartMs = Clock.now ();

			if ( fScale <= 0 )
			{
				throw new IllegalArgumentException ( "Time scale must be positive." );
			}
		}

		@Override
		public long nowMs ()
		{
			final long realNowMs = Clock.now ();
			final long actualDiffMs = realNowMs - fActualStartMs;
			final long scaledDiffMs = Math.round ( actualDiffMs * fScale );
			return fStartMs + scaledDiffMs;
		}

		public double getScale ()
		{
			return fScale;
		}

		private final long fStartMs;
		private final double fScale;
		private final long fActualStartMs;

		private static long makeStartTimeFrom ( long timeMs )
		{
			if ( timeMs < 0 )
			{
				// if negative, offset from now - e.g. use "-300000" for 5 minutes ago
				// note that we always start at at least time 1
				return Math.max ( 1, Clock.now () + timeMs );
			}
			return timeMs;
		}

		private static long getConfiguredStartTimeMs ( )
		{
			final String timeStart = System.getProperty ( skTimeStartMs );
			if ( StringUtils.isEmpty ( timeStart ) ) return System.currentTimeMillis ();

			try
			{
				return makeStartTimeFrom ( Long.parseLong ( timeStart ) );
			}
			catch ( NumberFormatException x )
			{
				log.warn ( "Couldn't parse timeStart: " + x.getMessage () );
			}
			return System.currentTimeMillis ();
		}

		private static double getConfiguredTimeScale ( )
		{
			final String timeScale = System.getProperty ( skTimeScaleArg );
			if ( StringUtils.isEmpty ( timeScale ) ) return 1L;

			try
			{
				final double scale = Double.parseDouble ( timeScale );
				if ( scale > 0 )
				{
					return scale;
				}

				log.warn ( "Time scale must be a positive number." );
			}
			catch ( NumberFormatException x )
			{
				log.warn ( "Couldn't parse {} {}, using 1.0", skTimeScaleArg, timeScale );
			}
			return 1.0;
		}
	}

	static final String skTimeStartMs = "continualClockStartMs";
	static final String skTimeScaleArg = "continualClockScale";
	private static final Logger log = LoggerFactory.getLogger ( Clock.class );
}
