package io.continual.jsonHttpClient.impl.cache;

import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.jsonHttpClient.JsonOverHttpClient.HttpResponse;
import io.continual.jsonHttpClient.ResponseCache;
import io.continual.util.data.HumanReadableHelper;
import io.continual.util.time.Clock;

/**
 *	A cache that's implemented over a ConcurrentMap to minimize read contention in 
 *	multi-threaded systems such as stream processing programs.
 */
public class ConcurrentMapCache implements ResponseCache, AutoCloseable
{
	public static final int kDefaultInitCapacity = 4096;
	public static final int kDefaultEstThreadCount = 8;
	public static final float kDefaultLoadFactor = 0.75f;
	public static final long kDefaultTimeoutMs = 1000L * 60 * 15;	// 15 minutes

	public static class Builder
	{
		public Builder withInitialCapacity ( int cap ) { fInitCap = cap; return this; }
		public Builder withLoadFactor ( float lf ) { fLoadFactor = lf; return this; }
		public Builder expectingThreadCount ( int threads ) { fThreadCount = threads; return this; }

		public Builder entriesTimingOutAfter ( long duration, TimeUnit units )
		{
			fTimeoutMs = TimeUnit.MILLISECONDS.convert ( duration, units );
			return this;
		}

		public Builder withoutTimeouts ()
		{
			fTimeoutMs = -1L;
			return this;
		}

		public Builder runningCleanupThread ()
		{
			fUseCleanupThread = true;
			return this;
		}

		public Builder withManualCleanup ()
		{
			fUseCleanupThread = false;
			return this;
		}

		public ConcurrentMapCache build ()
		{
			final ConcurrentMapCache cmc = new ConcurrentMapCache ( this );
			cmc.start ();
			return cmc;
		}

		private int fInitCap = kDefaultInitCapacity;
		private float fLoadFactor = kDefaultLoadFactor;
		private int fThreadCount = kDefaultEstThreadCount;
		private long fTimeoutMs = kDefaultTimeoutMs;
		private boolean fUseCleanupThread = true;
	}

	public ConcurrentMapCache ( Builder b )
	{
		fMap = new ConcurrentHashMap<> ( b.fInitCap, b.fLoadFactor, b.fThreadCount );
		fTimeoutMs = b.fTimeoutMs;
		fCleaner = ( b.fUseCleanupThread && b.fTimeoutMs >= 0 ) ? new Thread ()
			{
				@Override
				public void run ()
				{
					// roughly twice as often as a timeout range, but no faster than 5s and no longer than 5m
					final long everyMs = Math.min ( Math.max ( 1000L * 5, b.fTimeoutMs / 2 ), 1000L * 60 * 5 );
					final String everyStr = HumanReadableHelper.timeValue ( everyMs, TimeUnit.MILLISECONDS, 1000 );
					log.info ( "ConcurrentMapCache will cleanup every {}...", everyStr );

					// track actual times so that test clocks work properly
					final long startMs = Clock.now ();
					long nextRunAtMs = startMs + everyMs;

					while ( true )
					{
						try
						{
							Thread.sleep ( Math.max ( 1, nextRunAtMs - Clock.now () ) );

							final long now = Clock.now ();
							if ( now >= nextRunAtMs )
							{
								log.info ( "Culling cache for max 500 ms..." );
								final int removed = cull ( 500, TimeUnit.MILLISECONDS );
								log.info ( "Culled {} timed out items; next run in ~{}", removed, everyStr );
								nextRunAtMs = now + everyMs; 
							}
						}
						catch ( InterruptedException e )
						{
							log.info ( "Cache cleanup thread interrupted." );
							break;
						}
					}
					log.info ( "Cache cleanup thread exiting." );
				}
			} : null;
	}

	/**
	 * If the cleaner thread was created, start it
	 */
	public void start ()
	{
		if ( fCleaner != null )
		{
			fCleaner.start ();
		}
	}
	
	@Override
	public void close ()
	{
		log.info ( "Closing cache." );
		if ( fCleaner != null )
		{
			fCleaner.interrupt ();
		}
		fMap.clear ();
	}

	@Override
	public HttpResponse get ( String path )
	{
		final Entry e = fMap.get ( path );
		if ( e != null )
		{
			if ( !e.isTimedOut () )
			{
				return e.getResponse ();
			}
			else
			{
				fMap.remove ( path );
			}
		}
		return null;
	}

	@Override
	public void put ( String path, HttpResponse response )
	{
		fMap.put ( path, new Entry ( response ) );
	}

	@Override
	public void remove ( String path )
	{
		fMap.remove ( path );
	}

	/**
	 * Cull this cache of expired entries limited to the given duration. 
	 * @param maxDuration
	 * @param tu
	 */
	public int cull ( long maxDuration, TimeUnit tu )
	{
		int count = 0;

		final long timeLimit = Clock.now () + TimeUnit.MILLISECONDS.convert ( maxDuration, tu );
		final TreeSet<String> paths = new TreeSet<> ( fMap.keySet () );
		for ( String path : paths )
		{
			if ( Clock.now () > timeLimit ) return count;

			// It's possible a new entry is added between this read (get) and write (remove). That should
			// be rare and the consequence is just a potential cache miss later.

			final Entry e = fMap.get ( path );
			if ( e != null && e.isTimedOut () )
			{
				fMap.remove ( path );
				count++;
			}
		}

		return count;
	}

	private final ConcurrentHashMap<String,Entry> fMap;
	private final long fTimeoutMs;	// if <0, no timeouts
	private final Thread fCleaner;

	private class Entry
	{
		public Entry ( HttpResponse r )
		{
			fResponse = r;
			fGoodUntil = Clock.now () + fTimeoutMs;
		}
		
		public boolean isTimedOut ()
		{
			if ( fTimeoutMs < 0 ) return false;
			return Clock.now () > fGoodUntil;
		}

		public HttpResponse getResponse () { return fResponse; }

		private final HttpResponse fResponse;
		private final long fGoodUntil;
	}

	private static final Logger log = LoggerFactory.getLogger ( ConcurrentMapCache.class );
}
