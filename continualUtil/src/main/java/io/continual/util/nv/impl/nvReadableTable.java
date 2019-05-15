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
package io.continual.util.nv.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.continual.util.nv.NvReadable;

import java.util.Properties;
import java.util.TreeSet;

public class nvReadableTable extends nvBaseReadable implements NvReadable
{
	public nvReadableTable ()
	{
		this ( (Map<String,String>)null );
	}

	public nvReadableTable ( Map<String,String> content )
	{
		if ( content != null )
		{
			fTable = content;
		}
		else
		{
			fTable = new HashMap<String,String> ();
		}
	}

	public nvReadableTable ( Properties content )
	{
		fTable = new HashMap<String,String> ();
		for ( Entry<Object, Object> e : content.entrySet () )
		{
			fTable.put ( e.getKey().toString (), e.getValue ().toString () );
		}
	}

	public synchronized void clear ( String key )
	{
		fTable.remove ( key );
	}

	public synchronized void clear ()
	{
		fTable.clear ();
	}

	public synchronized boolean hasValueFor ( String key )
	{
		return fTable.containsKey ( key );
	}

	public synchronized String getString ( String key ) throws MissingReqdSettingException
	{
		final String result = fTable.get ( key );
		if ( result == null )
		{
			throw new MissingReqdSettingException ( key );
		}
		return result;
	}

	@Override
	public String[] getStrings ( String key ) throws MissingReqdSettingException
	{
		final String fullset = getString ( key );
		return fullset.split ( ",", -1 );
	}

	@Override
	public synchronized int size ()
	{
		return fTable.size ();
	}

	@Override
	public synchronized Collection<String> getAllKeys ()
	{
		final TreeSet<String> list = new TreeSet<String> ();
		for ( Object o : fTable.keySet () )
		{
			list.add ( o.toString () );
		}
		return list;
	}

	@Override
	public synchronized Map<String, String> getCopyAsMap ()
	{
		HashMap<String,String> map = new HashMap<String,String> ();
		for ( Entry<String, String> e : fTable.entrySet () )
		{
			map.put ( e.getKey(), e.getValue() );
		}
		return map;
	}

	protected synchronized void set ( String key, String val )
	{
		fTable.put ( key, val );
	}

	protected synchronized void set ( Map<String,String> map )
	{
		fTable.putAll ( map );
	}

	private final Map<String,String> fTable;
}
