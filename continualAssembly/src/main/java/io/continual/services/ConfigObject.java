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

package io.continual.services;

import java.util.Collection;
import java.util.TreeSet;

import org.json.JSONObject;

import io.continual.util.data.json.JsonUtil;

public class ConfigObject
{
	public ConfigObject ()
	{
		this ( new JSONObject () );
	}

	public ConfigObject ( JSONObject o )
	{
		fBase = null;
		fData = JsonUtil.clone ( o == null ? new JSONObject () : o );
	}

	public static ConfigObject read ( JSONObject o )
	{
		return new ConfigObject ( o );
	}

	@Override
	public String toString ()
	{
		return toJson().toString ( 4 );
	}

	public JSONObject toJson ()
	{
		JSONObject merged = fBase == null ? new JSONObject () : fBase.toJson ();
		JsonUtil.copyInto ( fData, merged );
		return merged;
	}

	public int size ()
	{
		return fData.length ();
	}

	public ConfigObject setBaseConfig ( ConfigObject co )
	{
		fBase = co;
		return this;
	}
	
	public String get ( String key )
	{
		return get ( key, null );
	}

	public String get ( String key, String defval )
	{
		if ( fData.has ( key ) )
		{
			return fData.getString ( key );
		}
		else if ( fBase != null )
		{
			return fBase.get ( key, defval );
		}
		return defval;
	}

	public boolean getBoolean ( String key )
	{
		return getBoolean ( key, false );
	}

	public boolean getBoolean ( String key, boolean defval )
	{
		if ( fData.has ( key ) )
		{
			return fData.optBoolean ( key );
		}
		else if ( fBase != null )
		{
			return fBase.getBoolean ( key, defval );
		}
		return defval;
	}

	public Collection<String> getAllKeys ()
	{
		final TreeSet<String> keys = new TreeSet<> ();
		if ( fBase != null )
		{
			keys.addAll ( fBase.getAllKeys () );
		}
		for ( Object key : fData.keySet () )
		{
			keys.add ( key.toString () );
		}
		return keys;
	}

	private final JSONObject fData;
	private ConfigObject fBase;

	protected void set ( String key, String value )
	{
		fData.put ( key, value );
	}

	protected ConfigObject getSubConfig ( String serviceName )
	{
		final JSONObject data = fData.optJSONObject ( serviceName );
		return data == null ? new ConfigObject () : new ConfigObject ( data );
	}
}
