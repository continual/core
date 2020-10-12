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
		mergeData ();
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
		return JsonUtil.clone ( fMerged );
	}

	public int size ()
	{
		return fMerged.length ();
	}

	public ConfigObject setBaseConfig ( ConfigObject co )
	{
		fBase = co;
		mergeData ();
		return this;
	}
	
	public String get ( String key )
	{
		return get ( key, null );
	}

	public String get ( String key, String defval )
	{
		if ( fMerged.has ( key ) )
		{
			return fMerged.getString ( key );
		}
		return defval;
	}

	public boolean getBoolean ( String key )
	{
		return getBoolean ( key, false );
	}

	public boolean getBoolean ( String key, boolean defval )
	{
		if ( fMerged.has ( key ) )
		{
			return fMerged.optBoolean ( key );
		}
		return defval;
	}

	public Collection<String> getAllKeys ()
	{
		final TreeSet<String> keys = new TreeSet<> ();
		for ( Object key : fMerged.keySet () )
		{
			keys.add ( key.toString () );
		}
		return keys;
	}

	private final JSONObject fData;
	private ConfigObject fBase;
	private JSONObject fMerged;

	protected void set ( String key, String value )
	{
		fData.put ( key, value );
		mergeData ();
	}

	protected ConfigObject getSubConfig ( String serviceName )
	{
		final JSONObject data = fMerged.optJSONObject ( serviceName );
		return data == null ? new ConfigObject () : new ConfigObject ( data );
	}

	private void mergeData ()
	{
		if ( fBase == null )
		{
			fMerged = JsonUtil.clone ( fData );
		}
		else
		{
			fMerged = JsonUtil.overlay ( JsonUtil.clone ( fBase.toJson () ), fData );
		}
	}
}
