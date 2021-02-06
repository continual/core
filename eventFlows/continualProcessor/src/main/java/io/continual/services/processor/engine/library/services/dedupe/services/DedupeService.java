package io.continual.services.processor.engine.library.services.dedupe.services;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.service.SimpleProcessingService;
import io.continual.util.time.Clock;

public class DedupeService extends SimpleProcessingService
{
	public DedupeService ( ConfigLoadContext sc, JSONObject config )
	{
		fLastSeenMs = new HashMap<> ();
		fTimeOrder = new LinkedList<> ();
		fMaxAgeMs = config.optLong ( "maxAgeMs", Long.MAX_VALUE );
		fMaxSize = config.optLong ( "maxSize", Long.MAX_VALUE );
		fBackgroundProcessing = Executors.newScheduledThreadPool ( 1 );
	}

	@Override
	protected void onStart ()
	{
		fBackgroundProcessing.scheduleAtFixedRate ( new Runnable ()
		{
			@Override
			public void run ()
			{
				cull ();
			}
		}, 5, 5, TimeUnit.SECONDS );
	}

	@Override
	protected void onStopRequested ()
	{
		fBackgroundProcessing.shutdown ();
	}

	public synchronized boolean exists ( String key )
	{
		cull ();
		return fLastSeenMs.containsKey ( key );
	}

	public synchronized void add ( String key )
	{
		cull ();

		final Entry e = new Entry ( key );
		fLastSeenMs.put ( key, e );
		fTimeOrder.add ( e );
	}

	public synchronized void remove ( String key )
	{
		fLastSeenMs.remove ( key );
		cull ();
	}

	private synchronized void cull ()
	{
		final long oldestMs = Clock.now () - fMaxAgeMs;
		while ( fTimeOrder.size () > 0 && ( fTimeOrder.peek ().fTimeMs < oldestMs || fTimeOrder.size () > fMaxSize ) )
		{
			final Entry e = fTimeOrder.remove ( 0 );
			final Entry active = fLastSeenMs.get ( e.fKey );
			if ( e == active )	// same object
			{
				fLastSeenMs.remove ( e.fKey );
			}
			// else: stale reference
		}
	}

	private final HashMap<String,Entry> fLastSeenMs;
	private final LinkedList<Entry> fTimeOrder;
	private final long fMaxAgeMs;
	private final long fMaxSize;
	private final ScheduledExecutorService fBackgroundProcessing;

	private static class Entry
	{
		public Entry ( String key )
		{
			this ( key, Clock.now () );
		}

		public Entry ( String key, long timeMs )
		{
			fKey = key;
			fTimeMs = timeMs;
		}
		public final String fKey;
		public final long fTimeMs;
	}
}
