package io.continual.util.collections;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.time.Clock;

/**
 * This cache aims to provide safe concurrent access to cached data while allowing some
 * longer operations to run without blocking the entire cache. Keys are hashed into a shard
 * number and blocking operations for a given key block only the shard.  This cache was built
 * mainly to cache response data in a scenario where the same key is requested repeatedly across
 * multiple calling threads and the response itself can take some time to acquire. The result
 * is a large number of overlapping read-then-write operations for a given key. This cache
 * will route each to the same synchronized shard object to block the operation while the first thread
 * completes the fetch.  Other keys in the same shard are also blocked, so a trade-off is made
 * between memory used and shard count.
 */
public class ShardedExpiringCache<K,V>
{
	/**
	 * Cache entries can be judged to be stale separately from their age. For example,
	 * a credentials instance might store a hash of the cache credentials that doesn't match
	 * user input.
	 *
	 * @param <V>
	 */
	public interface Validator<V>
	{
		/**
		 * Is the value from the cache still valid for use?
		 * @param value
		 * @return true if still valid
		 */
		boolean isValid ( V value );
	}

	/**
	 * The cache can optionally fetch data on misses via the Fetcher interface.
	 *
	 * @param <K>
	 * @param <V>
	 */
	public interface Fetcher<K,V>
	{
		public static class FetchException extends Exception
		{
			public FetchException ( String msg ) { super(msg); }
			public FetchException ( Throwable t ) { super(t); }
			private static final long serialVersionUID = 1L;
		};

		/**
		 * Fetch the value for the given key from the backing data store
		 * @param key
		 * @return a value, or null if the value is unavailable
		 * @throws FetchException
		 */
		V fetch ( K key ) throws FetchException;
	}

	/**
	 * Monitor particular events, mainly to feed a higher-level metrics registry.
	 */
	public interface Monitor
	{
		default void onCacheHit () {};
		default void onCacheMiss () {};
	}
	
	/**
	 * Build a cache
	 *
	 * @param <K>
	 * @param <V>
	 */
	public static class Builder<K,V>
	{
		public Builder<K,V> named ( String name ) { fName = name; return this; }

		public Builder<K,V> cachingFor ( long duration, TimeUnit timeUnit ) { fDurMs = TimeUnit.MILLISECONDS.convert ( duration, timeUnit ); return this; }

		public Builder<K,V> withShardCount ( int shardCount ) { fShardCount = shardCount; return this; }

		public Builder<K,V> withShardMaxSize ( int shardMaxSize ) { fShardMaxSize = shardMaxSize; return this; }

		public Builder<K,V> notificationsTo ( Monitor m ) { fMonitor = m; return this; }

		public ShardedExpiringCache<K,V> build ()
		{
			return new ShardedExpiringCache<K,V> ( this );
		}

		private int fShardCount = kDefaultShardCount;
		private int fShardMaxSize = kDefaultShardMaxSize;
		private Monitor fMonitor = new Monitor () {};
		private String fName = "(unnamed)";
		private long fDurMs = 15*60*1000L;
	}

	/**
	 * Does the cache contain the given key?
	 * @param key
	 * @return true if the cache contains the key, per read(key)
	 */
	public boolean containsKey ( K key )
	{
		return read ( key ) != null;
	}
	
	/**
	 * Does the cache contain the given key?
	 * @param key
	 * @param validator
	 * @return true if the cache contains the key, per read(key,validator)
	 */
	public boolean containsKey ( K key, Validator<V> validator )
	{
		return read ( key, validator ) != null;
	}

	/**
	 * Read a value by key, without additional validation or fetch.
	 * @param key
	 * @return a value, if available
	 */
	public V read ( K key )
	{
		return read ( key, null );
	}

	/**
	 * Read a value by key with additional validation if a validator is provided.
	 * @param key
	 * @param validator an optional validator
	 * @return a value, if available and valid
	 */
	public V read ( K key, Validator<V> validator )
	{
		try
		{
			return read ( key, validator, null );
		}
		catch ( Fetcher.FetchException e )
		{
			throw new RuntimeException ( "Null fetcher caused fetch exception?", e );
		}
	}

	/**
	 * Read a value by key, optionally validate it, and, if not found in the cache, optionally
	 * fetch it from the backing store. Note that the validator is used only for a cached value. A 
	 * value from the fetcher is assumed to be valid.
	 *  
	 * @param key
	 * @param validator an optional validator
	 * @param fetcher an optional fetcher
	 * @return a value, if available either in the cache or the backing store, and validated by the validator.
	 * @throws Fetcher.FetchException
	 */
	public V read ( K key, Validator<V> validator, Fetcher<K,V> fetcher ) throws Fetcher.FetchException
	{
		try ( ShardCallWrap lw = new ShardCallWrap ( "for read" ) )
		{
			return getShard ( key ).read ( key, validator, fetcher );
		}
	}

	/**
	 * Write a value for a key into the cache using the default cache duration.
	 * @param key
	 * @param val
	 */
	public void write ( K key, V val )
	{
		write ( key, val, fDurMs );
	}

	/**
	 * Write a value for a key into the cache using the given cache duration.
	 * @param key
	 * @param val
	 * @param cacheDurationMs
	 */
	public void write ( K key, V val, long cacheDurationMs )
	{
		try ( ShardCallWrap lw = new ShardCallWrap ( "for write" ) )
		{
			getShard ( key ).write ( key, val, cacheDurationMs );
		}
	}

	/**
	 * Remove a key from the cache.
	 * @param key
	 */
	public void remove ( K key )
	{
		try ( ShardCallWrap lw = new ShardCallWrap ( "for object removal" ) )
		{
			getShard ( key ).remove ( key );
		}
	}

	/**
	 * Emptying the cache is not guaranteed to happen atomically. For example,
	 * new items may appear in the cache before the sweep is complete.
	 */
	public void empty ()
	{
		for ( int i=0; i<fShardCount; i++ )
		{
			fShards.get ( i ).empty ();
		}
	}

	/**
	 * Get the (approximate) size of the cache
	 * @return the cache size
	 */
	public int size ()
	{
		int size = 0;
		for ( int i=0; i<fShardCount; i++ )
		{
			size += fShards.get ( i ).size ();
		}
		return size;
	}

	private final String fName;
	private final long fDurMs;
	private final int fShardCount;
	private final int fShardMaxSize;
	private final ArrayList<Shard> fShards;
	private final Monitor fMonitor;

	private Shard getShard ( K key )
	{
		final int hash = Math.abs ( key.hashCode () );
		final int index = hash % fShardCount;
		return fShards.get ( index );
	}
	
	private class Shard
	{
		public Shard ( int id )
		{
			fId = id;
			fItemCache = new HashMap<> ();
			fItemCleanups = new LinkedList<> ();
		}

		public synchronized V read ( K key, Validator<V> validator, Fetcher<K,V> fetcher ) throws Fetcher.FetchException
		{
			cleanupCache ();

			final CacheEntry e = fItemCache.get ( key );
			if ( e != null )
			{
				final V val = e.getValue ();
				if ( val != null && ( validator == null || validator.isValid ( val ) ) )
				{
					fMonitor.onCacheHit ();
					log.info ( "Read/returned {} from cache {}/{}.", key.toString (), fName, fId );
					return val;
				}
				else
				{
					// else: this entry timed out. isn't valid, or was garbage collected
					log.debug ( "Read {} from cache {}/{}, but {}.", key.toString (), fName, fId,
						( val == null ? "it was cleaned up" : "it's not valid" ) );
					fItemCache.remove ( key );
				}
			}
			log.debug ( "No valid entry for {} in cache {}/{}.", key.toString (), fName, fId );

			fMonitor.onCacheMiss ();

			if ( fetcher != null )
			{
				log.info ( "Cache fetching {} from backing store in shard {}/{}", key, fName, fId );
				final V fetched = fetcher.fetch ( key );
				if ( fetched != null )
				{
					write ( key, fetched );
					return fetched;
				}
			}

			return null;
		}

		public synchronized void write ( K key, V val )
		{
			write ( key, val, fDurMs );
		}

		public synchronized void write ( K key, V val, long cacheDurationMs )
		{
			// we need a key
			if ( key == null )
			{
				log.warn ( "Ignoring null key insert in cache {}.", fName );
				return;
			}

			// ignore if duration is zero
			if ( cacheDurationMs <= 0L ) return;

			// trim old entries as needed
			while ( fShardMaxSize > 0 && size() >= fShardMaxSize )
			{
				// remove the oldest item
				cleanupCacheItem ( fItemCleanups.remove () );
			}

			// wrap the value in our cache entry and insert it
			final CacheEntry ce = new CacheEntry ( key, val, now() + cacheDurationMs );
			fItemCache.put ( key, ce );
			fItemCleanups.add ( ce );

			log.debug ( "Wrote {} to cache {}.", key.toString (), fName );
		}

		public synchronized void remove ( K key )
		{
			fItemCache.remove ( key );
		}

		public synchronized void empty ()
		{
			fItemCache.clear ();
			fItemCleanups.clear ();
		}

		public synchronized int size ()
		{
			return fItemCache.size ();
		}

		private synchronized void $testGc ( K key )
		{
			final CacheEntry ce = fItemCache.get ( key );
			if ( ce != null )
			{
				ce.$testGc ();
			}
		}

		private final int fId;
		private final HashMap<K, CacheEntry> fItemCache;
		private final LinkedList<CacheEntry> fItemCleanups;	// time-ordered list of entries created

		private void cleanupCacheItem ( CacheEntry cleanupEntry )
		{
			final CacheEntry cacheEntry = fItemCache.get ( cleanupEntry.getKey () );
			if ( cacheEntry != null && cacheEntry == cleanupEntry )
			{
				// the cache entry is still the same entry as the cleanup task entry, so remove from cache
				fItemCache.remove ( cleanupEntry.getKey() );
				log.info ( "Removed cache entry for \"{}\" in cache {}.", cleanupEntry.toString (), fName );
			}
			else
			{
				log.debug ( "Removed cleanup entry for \"{}\" in cache {}, but no match in item cache.", cleanupEntry.toString (), fName );
			}
		}

		private void cleanupCache ()
		{
			final long now = now ();
			while ( fItemCleanups.size () > 0 && fItemCleanups.get ( 0 ).getExpiresAtMs() < now )
			{
				cleanupCacheItem ( fItemCleanups.remove () );
			}
		}
	}

	private class CacheEntry
	{
		public CacheEntry ( K key, V val, long expiresAtMs )
		{
			fKey = key;
			fVal = new SoftReference<V> ( val );
			fExpiresAtMs = expiresAtMs;
		}

		public K getKey () { return fKey; }
		
		public V getValue () { return fVal.get (); }

		public long getExpiresAtMs () { return fExpiresAtMs; }
		
		@Override
		public String toString () { return fKey.toString (); }
	
		private void $testGc () { fVal.clear (); }

		private final K fKey;
		private final SoftReference<V> fVal;
		private final long fExpiresAtMs;
	}

	private ShardedExpiringCache ( Builder<K,V> b )
	{
		fName = b.fName;
		fDurMs = b.fDurMs;
		fShardCount = b.fShardCount;
		fShardMaxSize = b.fShardMaxSize;
		fShards = new ArrayList<Shard> ( fShardCount );
		for ( int i=0; i<fShardCount; i++ )
		{
			fShards.add ( new Shard ( i ) );
		}

		fMonitor = b.fMonitor;
	}

	private static class ShardCallWrap implements AutoCloseable
	{
		public ShardCallWrap ( String msg )
		{
			log.debug ( "locking shard " + msg );
		}

		@Override
		public void close ()
		{
			final long stopTimeMs = now ();
			final long durationMs = stopTimeMs - fStartTimeMs;
			log.debug ( "shard released, {} ms", durationMs );

			// basic awareness of trouble, esp. if a fetch call takes a long time
			if ( durationMs > skWarnOnLockDurationMs )
			{
				log.warn ( "Shard call wrap took {} ms", durationMs );
			}
		}

		private final long fStartTimeMs = now ();
	}
	
	static long now ()
	{
		return Clock.now ();
	}

	private static final int kDefaultShardCount = 1024;
	private static final int kDefaultShardMaxSize = 4 * 1024;
	private static final long skWarnOnLockDurationMs = 10 * 1000L;

	private static final Logger log = LoggerFactory.getLogger ( ShardedExpiringCache.class );

	void $testDropWeakRef ( K key )
	{
		try ( ShardCallWrap lw = new ShardCallWrap ( "for gc test" ) )
		{
			getShard ( key ).$testGc ( key );
		}
	}
}
