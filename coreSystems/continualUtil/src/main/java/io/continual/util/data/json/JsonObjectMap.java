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

package io.continual.util.data.json;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonObjectMap implements Map<String,String>
{
	public JsonObjectMap ( JSONObject o )
	{
		fObject = o;
	}
	
	@Override
	public int size ()
	{
		return fObject.length ();
	}

	@Override
	public boolean isEmpty ()
	{
		return size() == 0;
	}

	@Override
	public boolean containsKey ( Object key )
	{
		return fObject.has ( key.toString () );
	}

	@Override
	public boolean containsValue ( Object value )
	{
		return values().contains ( value );
	}

	@Override
	public String get ( Object key )
	{
		return fObject.optString ( key.toString () );
	}

	@Override
	public String put ( String key, String value )
	{
		final String was = fObject.optString ( key );
		fObject.put ( key, value );
		return was;
	}

	@Override
	public String remove ( Object key )
	{
		final String was = fObject.optString ( key.toString () );
		fObject.remove ( key.toString () );
		return was;
	}

	@Override
	public void putAll ( Map<? extends String, ? extends String> m )
	{
		for ( Map.Entry<? extends String, ? extends String> e : m.entrySet () )
		{
			put ( e.getKey(), e.getValue () );
		}
	}

	@Override
	public void clear ()
	{
		while (fObject.keySet().iterator().hasNext())
		{
			fObject.remove(fObject.keySet().iterator().next());
		}
	}

	@Override
	public Set<String> keySet ()
	{
		final TreeSet<String> set = new TreeSet<String> ();
		for ( Map.Entry<String, String> e : entrySet () )
		{
			set.add ( e.getKey () );
		}
		return set;
	}

	@Override
	public Collection<String> values ()
	{
		final TreeSet<String> set = new TreeSet<String> ();
		for ( Map.Entry<String, String> e : entrySet () )
		{
			set.add ( e.getValue () );
		}
		return set;
	}

	@Override
	public Set<Map.Entry<String, String>> entrySet ()
	{
		final TreeSet<Map.Entry<String, String>> result = new TreeSet<Map.Entry<String, String>> ();
		try
		{
			final JSONArray names = fObject.names ();
			for ( int i=0; i<names.length (); i++ )
			{
				final String key = names.getString ( i );
				final Object val = fObject.opt ( key );
				if ( val != null )
				{
					result.add ( new entry ( key, val.toString () ) );
				}
			}
		}
		catch ( JSONException e )
		{
			throw new RuntimeException ( e );
		}
		return result;
	}

	private final JSONObject fObject;

	private static class entry implements Map.Entry<String,String> 
	{
		private final String fK, fV;

		public entry ( String k, String v )
		{
			fK = k; fV = v;
		}

		@Override
		public String getKey ()
		{
			return fK;
		}

		@Override
		public String getValue ()
		{
			return fV;
		}

		@Override
		public String setValue ( String value )
		{
			return null;
		}
		
	};
}
