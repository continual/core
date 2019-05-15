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
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeSet;

import io.continual.util.nv.NvReadable;

public class nvReadableStack extends nvBaseReadable implements NvReadable
{
	public nvReadableStack ()
	{
		super ();
		fStack = new LinkedList<NvReadable> ();
	}

	public nvReadableStack push ( NvReadable p )
	{
		fStack.addFirst ( p );
		return this;
	}

	public nvReadableStack pushBelow ( NvReadable below, NvReadable above )
	{
		int i = fStack.indexOf ( above );
		if ( i < 0 )
		{
			push ( below );
		}
		else
		{
			fStack.add ( i+1, below );
		}
		return this;
	}

	public String getString ( String key ) throws MissingReqdSettingException
	{
		String result = null;
		boolean found = false;
		for ( NvReadable p : fStack )
		{
			if ( p.hasValueFor ( key ) )
			{
				result = p.getString ( key );
				found = true;
				break;
			}
		}

		if ( !found )
		{
			throw new MissingReqdSettingException ( key );
		}

		return eval ( result );
	}

	// different implementations handle string sets differently; we can't assume that getString returns a comma-delimited array
	@Override
	public String[] getStrings ( String key ) throws MissingReqdSettingException
	{
		String[] result = null;
		boolean found = false;
		for ( NvReadable p : fStack )
		{
			if ( p.hasValueFor ( key ) )
			{
				result = p.getStrings ( key );
				found = true;
				break;
			}
		}

		if ( !found )
		{
			throw new MissingReqdSettingException ( key );
		}

		if ( result != null )
		{
			for ( int i=0; i<result.length; i++ )
			{
				result[i] = eval ( result[i] );
			}
		}
		return result;
	}

	public boolean hasValueFor ( String key )
	{
		boolean result = false;
		for ( NvReadable p : fStack )
		{
			result = p.hasValueFor ( key );
			if ( result ) break;
		}
		return result;
	}

	public void rescan () throws LoadException
	{
		for ( NvReadable p : fStack )
		{
			p.rescan ();
		}
	}

	private final LinkedList<NvReadable> fStack;

	@Override
	public int size ()
	{
		return getAllKeys().size ();
	}

	@Override
	public Collection<String> getAllKeys ()
	{
		final TreeSet<String> set = new TreeSet<String> ();
		for ( NvReadable r : fStack )
		{
			set.addAll ( r.getAllKeys () );
		}
		return set;
	}

	@Override
	public Map<String, String> getCopyAsMap ()
	{
		// this could be faster, but it's an easy way to get the correct values
		final HashMap<String,String> map = new HashMap<String,String> ();
		for ( String key : getAllKeys () )
		{
			final String val = getString ( key, null );
			if ( val != null )
			{
				map.put ( key, val );
			}
		}
		return map;
	}
}
