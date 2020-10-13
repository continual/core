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
package io.continual.util.collections;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

public class LruCache <K extends Object, T extends Object>
{
	public interface ExpulsionListener<K,T>
	{
		void onExpelled ( K key, T value );
	}

	/**
	 * Construct an LRU Cache with the given max size. 
	 * @param maxSize the maximum number of entries in the cache
	 */
	public LruCache ( long maxSize )
	{
		fEntries = new Hashtable<K,Entry> ();
		fMruList = new LinkedList<K> ();

		setMaxSize ( maxSize );
	}

	public T get ( K key )
	{
		return get ( key, -1 );
	}

	public T get ( K key, long maxAgeMs )
	{
		return lookup ( key, maxAgeMs );
	}

	public synchronized T lookup ( K key )
	{
		return lookup ( key, -1 );
	}

	/**
	 * Lookup the value for a key. If a max age is specified (&lt;-1) and the
	 * entry exists but is older than the max age, the entry is removed from
	 * the cache.
	 * 
	 * @param key a key to search for
	 * @param maxAgeMs the max age allowed for the value
	 * @return a value for the given key or null if no value exists within the time range specified
	 */
	public synchronized T lookup ( K key, long maxAgeMs )
	{
		T t = null;
		Entry e = fEntries.get ( key );
		if ( e != null )
		{
			if ( maxAgeMs > -1 )
			{
				final long now = System.currentTimeMillis ();
				final long age = now - e.initialInsertMs;
				if ( age > maxAgeMs )
				{
					remove ( key );
					return null;
				}
			}
			
			t = e.value;
			noteUse ( key );
		}
		return t;
	}

	public T put ( K key, T object )
	{
		return store ( key, object );
	}

	public T store ( K key, T object )
	{
		return store ( key, object, null );
	}
	
	public synchronized T store ( K key, T object, ExpulsionListener<K,T> el )
	{
		ensureCapacity ();
		if ( fEntries.size() < fMaxSize )	// max size can be 0
		{
			final Entry e = new Entry ();
			e.value = object;
			e.expulsion = el;
			e.initialInsertMs = System.currentTimeMillis ();
			final Entry was = fEntries.put ( key, e );
			noteUse ( key );
			return was == null ? null : was.value;
		}
		return null;
	}

	public synchronized T remove ( Object key )
	{
		fMruList.remove ( key );
		final Entry e = fEntries.remove ( key );
		return e == null ? null : e.value;
	}

	public synchronized void drop ( K key )
	{
		remove ( key );
	}

	public synchronized int size ()
	{
		return fEntries.size ();
	}
	
	public synchronized long maxSize ()
	{
		return fMaxSize;
	}

	public synchronized void setMaxSize ( long size )
	{
		if ( size < 0 )
		{
			size = 0;
		}
		fMaxSize = size;
		ensureCapacity ();
	}

	/**
	 * Clear the cache of all entries. Note that this version does not call expulsion listeners.
	 */
	public void clear ()
	{
		clear ( false );
	}

	/**
	 * Clear the cache of all entries, optionally calling the expulsion listeners associated with them.
	 * @param callExpulsionListeners if true, explusion listeners are notitifed
	 */
	public synchronized void clear ( boolean callExpulsionListeners )
	{
		if ( callExpulsionListeners )
		{
			for ( K key : fMruList )
			{
				final Entry e = fEntries.get ( key );
				if ( e != null && e.expulsion != null )
				{
					e.expulsion.onExpelled ( key, e.value );
				}
			}
		}
		fEntries.clear ();
		fMruList.clear ();
	}

	public boolean isEmpty ()
	{
		return size () == 0;
	}

	public boolean containsKey ( K key )
	{
		return lookup ( key, -1 ) != null;
	}

	public synchronized Set<K> keys ()
	{
		return new TreeSet<> ( fMruList );
	}
	
	private long fMaxSize;
	private final Hashtable<K,Entry> fEntries;
	private final LinkedList<K> fMruList;	// first item is MRU

	private void noteUse ( K key )
	{
		fMruList.remove ( key );	// FIXME: this is likely O(n)
		fMruList.addFirst ( key );
	}

	private class Entry
	{
		T value;
		ExpulsionListener<K,T> expulsion;
		long initialInsertMs;
	}

	private void ensureCapacity ()
	{
		while ( fEntries.size() >= fMaxSize && fEntries.size() != 0 )
		{
			final K key = fMruList.removeLast ();
			final Entry e = fEntries.remove ( key );
			if ( e.expulsion != null )
			{
				e.expulsion.onExpelled ( key, e.value );
			}
		}
	}
}
