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

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Maps a key to a list (not just a set) of values. The classes used for
 * K and V are not required to be immutable, but if they're not, clone()
 * could create a map that's not completely independent in terms of the
 * values stored.
 *
 * @param <K>
 * @param <V>
 */
public class MultiMap<K,V>
{
	public MultiMap ()
	{
		fMultiMap = new Hashtable<K,List<V>> ();
	}

	public MultiMap ( Map<K,V> data )
	{
		this ();

		for ( Map.Entry<K,V> e : data.entrySet () )
		{
			put ( e.getKey (), e.getValue () );
		}
	}

	@Override
	public MultiMap<K,V> clone ()
	{
		final MultiMap<K,V> newMap = new MultiMap<> ();
		for ( Entry<K, List<V>> e : fMultiMap.entrySet () )
		{
			final K key = e.getKey ();

			// get(key) returns a new list
			newMap.put ( key, get ( key ) );
		}
		return newMap;
	}
	
	public synchronized void put ( K k )
	{
		getOrCreateFor ( k );
	}

	public synchronized void put ( K k, V v )
	{
		LinkedList<V> list = new LinkedList<V>();
		list.add ( v );
		put ( k, list );
	}

	public synchronized void put ( K k, Collection<V> v )
	{
		List<V> itemList = getOrCreateFor ( k );
		itemList.removeAll ( v );	// only one of a given value allowed
		itemList.addAll ( v );
	}

	public synchronized void putAll ( Map<K,? extends Collection<V>> values )
	{
		for ( Map.Entry<K,? extends Collection<V>> e : values.entrySet () )
		{
			put ( e.getKey (), e.getValue () );
		}
	}
	
	public synchronized boolean containsKey ( K k )
	{
		return fMultiMap.containsKey ( k );
	}

	/**
	 * Get the values for a given key. A list is always returned, but it may be empty.
	 * @param k
	 * @return
	 */
	public synchronized List<V> get ( K k )
	{
		List<V> itemList = new LinkedList<V> ();
		if ( fMultiMap.containsKey ( k ) )
		{
			itemList = getOrCreateFor ( k );
		}
		return new LinkedList<V> ( itemList );
	}

	/**
	 * Get the first value for the given key, or return null if none exists.
	 * @param k
	 * @return
	 */
	public V getFirst ( K k )
	{
		final List<V> items = get ( k );
		if ( items.size () > 0 )
		{
			return items.get ( 0 );
		}
		return null;
	}

	public synchronized Collection<K> getKeys ()
	{
		return fMultiMap.keySet ();
	}

	public synchronized Map<K,List<V>> getValues ()
	{
		return fMultiMap;
	}

	public synchronized Map<K,Collection<V>> getCopyAsSimpleMap ()
	{
		final HashMap<K,Collection<V>> result = new HashMap<K,Collection<V>> ();
		for ( Entry<K, List<V>> e : fMultiMap.entrySet () )
		{
			final LinkedList<V> list = new LinkedList<V> ();
			list.addAll ( e.getValue () );
			result.put ( e.getKey(), list );
		}
		return result;
	}

	public synchronized List<V> remove ( K k )
	{
		return fMultiMap.remove ( k );
	}

	public synchronized void remove ( K k, V v )
	{
		List<V> itemList = getOrCreateFor ( k );
		itemList.remove ( v );
	}

	public synchronized void clear ()
	{
		fMultiMap.clear ();
	}

	public synchronized int size ()
	{
		return fMultiMap.size ();
	}

	public synchronized int size ( K k )
	{
		return getOrCreateFor ( k ).size ();
	}

	private final Hashtable<K,List<V>> fMultiMap;

	private synchronized List<V> getOrCreateFor ( K k )
	{
		List<V> itemList = fMultiMap.get ( k );
		if ( itemList == null )
		{
			itemList = new LinkedList<V> ();
			fMultiMap.put ( k, itemList );
		}
		return itemList;
	}
}
