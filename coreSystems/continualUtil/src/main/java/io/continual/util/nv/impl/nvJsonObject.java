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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.JsonEval;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.nv.NvWriteable;

public class nvJsonObject implements NvWriteable, JsonSerialized
{
	public nvJsonObject ()
	{
		fObject = new JSONObject ();
	}

	public nvJsonObject ( JSONObject o )
	{
		fObject = o;
	}

	@Override
	public JSONObject toJson ()
	{
		return JsonUtil.clone ( fObject );
	}

	@Override
	public String toString ()
	{
		return fObject.toString ();
	}

	@Override
	public String get ( String key )
	{
		return getString ( key, null );
	}

	@Override
	public String getString ( String key ) throws MissingReqdSettingException
	{
		final String result = getString ( key, null );
		if ( result == null ) throw new MissingReqdSettingException ( key );
		return result;
	}

	@Override
	public String getString ( String key, String defValue )
	{
		try
		{
			final Object o = JsonEval.eval ( fObject, key );
			if ( o != null )
			{
				return o.toString ();
			}
		}
		catch ( JSONException e )
		{
			// ignore
		}
		return defValue;
	}

	@Override
	public char getCharacter ( String key ) throws MissingReqdSettingException
	{
		try
		{
			final String s = getString ( key );
			if ( s.length () == 1 )
			{
				return s.charAt ( 0 );
			}
			throw new MissingReqdSettingException ( key );
		}
		catch ( JSONException e )
		{
			throw new MissingReqdSettingException ( key );
		}
	}

	@Override
	public char getCharacter ( String key, char defValue )
	{
		try
		{
			return getCharacter ( key );
		}
		catch ( MissingReqdSettingException e )
		{
			return defValue;
		}
	}

	@Override
	public boolean getBoolean ( String key ) throws MissingReqdSettingException
	{
		return getGenericValue ( key, Boolean.FALSE );
	}

	@Override
	public boolean getBoolean ( String key, boolean defValue )
	{
		return optGenericValue ( key, defValue );
	}

	@Override
	public int getInt ( String key ) throws MissingReqdSettingException
	{
		return getGenericValue ( key, 0 );
	}

	@Override
	public int getInt ( String key, int defValue )
	{
		return optGenericValue ( key, defValue );
	}

	@Override
	public long getLong ( String key ) throws MissingReqdSettingException
	{
		return getGenericValue ( key, 0L );
	}

	@Override
	public long getLong ( String key, long defValue )
	{
		return optGenericValue ( key, defValue );
	}

	@Override
	public double getDouble ( String key ) throws MissingReqdSettingException
	{
		return getGenericValue ( key, 0.0d );
	}

	@Override
	public double getDouble ( String key, double defValue )
	{
		return optGenericValue ( key, defValue );
	}

	@Override
	public byte[] getBytes ( String key ) throws MissingReqdSettingException, InvalidSettingValueException
	{
		return TypeConvertor.hexStringToBytes ( getString ( key ) );
	}

	@Override
	public byte[] getBytes ( String key, byte[] defValue )
	{
		try
		{
			return getBytes ( key );
		}
		catch ( MissingReqdSettingException e )
		{
			return defValue;
		}
		catch ( InvalidSettingValueException e )
		{
			return defValue;
		}
	}

	@Override
	public String[] getStrings ( String key ) throws MissingReqdSettingException
	{
		final JSONArray val = getGenericValue ( key, new JSONArray () );
		return JsonVisitor.arrayToList ( val ).toArray ( new String[val.length ()] );
	}

	@Override
	public String[] getStrings ( String key, String[] defValue )
	{
		try
		{
			return getStrings ( key );
		}
		catch ( MissingReqdSettingException x )
		{
			return defValue;
		}
	}

	@Override
	public int size ()
	{
		return getAllKeys ().size ();
	}

	@Override
	public boolean hasValueFor ( String key )
	{
		return getAllKeys ().contains ( key );
	}

	@Override
	public synchronized Collection<String> getAllKeys ()
	{
		if ( fKeyCache == null )
		{
			fKeyCache = new TreeSet<String> ();
			getKeys ( fKeyCache, "", fObject );
		}
		return fKeyCache;
	}

	@Override
	public Map<String, String> getCopyAsMap ()
	{
		final HashMap<String,String> result = new HashMap<String,String> ();
		copyInto ( result );
		return result;
	}

	@Override
	public void copyInto ( NvWriteable writeable )
	{
		for ( String key : getAllKeys () )
		{
			try
			{
				final Object o = JsonEval.eval ( fObject, key );
				if ( o instanceof Integer )
				{
					writeable.set ( key, (Integer) o );
				}
				else if ( o instanceof Long )
				{
					writeable.set ( key, (Long) o );
				}
				else if ( o instanceof Double )
				{
					writeable.set ( key, (Double) o );
				}
				else if ( o instanceof Boolean )
				{
					writeable.set ( key, (Boolean) o );
				}
				else if ( o instanceof JSONArray )
				{
					writeable.set ( key, getStrings ( key ) );
				}
				else
				{
					writeable.set ( key, (String) o );
				}
				// FIXME: byte[] and char are converted to strings here
			}
			catch ( JSONException e )
			{
				throw new RuntimeException ( "error copying value for " + key, e ); 
			}
			catch ( MissingReqdSettingException e )
			{
				throw new RuntimeException ( "error copying value for " + key, e ); 
			}
		}
	}

	@Override
	public void copyInto ( Map<String, String> writeable )
	{
		for ( String key : getAllKeys () )
		{
			try
			{
				writeable.put ( key, JsonEval.eval ( fObject, key ).toString () );
			}
			catch ( JSONException e )
			{
				throw new RuntimeException ( "error copying value for " + key, e ); 
			}
		}
	}

	@Override
	public void rescan ()
	{
		// nothing to do
	}

	@Override
	public void clear ()
	{
		for ( String key : getAllKeys () )
		{
			fObject.remove ( key );
		}
	}

	@Override
	public synchronized void set ( String key, String value )
	{
		fObject.put ( key, value );
	}

	@Override
	public void set ( String key, char value )
	{
		fObject.put ( key, "" + value );
	}

	public synchronized void set ( String key, int value )
	{
		fObject.put ( key, value );
	}

	public synchronized void set ( String key, long value )
	{
		fObject.put ( key, value );
	}

	public synchronized void set ( String key, double value )
	{
		fObject.put ( key, value );
	}

	public synchronized void set ( String key, boolean value )
	{
		fObject.put ( key, value );
	}

	public synchronized void set ( Map<String,String> map )
	{
		for ( Map.Entry<String, String> e : map.entrySet () )
		{
			fObject.put ( e.getKey(), e.getValue() );
		}
	}

	@Override
	public synchronized void unset ( String key )
	{
		fObject.remove ( key );
	}

	@Override
	public synchronized void set ( String key, byte[] value )
	{
		set ( key, value, 0, value.length );
	}

	@Override
	public synchronized void set ( String key, byte[] value, int offset, int length )
	{
		set ( key, TypeConvertor.bytesToHex ( value, offset, length ) );
	}

	@Override
	public void set ( String key, String[] values )
	{
		final JSONArray a = new JSONArray ();
		for ( String s : values )
		{
			a.put ( s );
		}
		fObject.put ( key, a );
	}

	public void set ( String key, List<String> values )
	{
		final JSONArray a = new JSONArray ();
		for ( String s : values )
		{
			a.put ( s );
		}
		fObject.put ( key, a );
	}

	private final JSONObject fObject;
	private TreeSet<String> fKeyCache = null;

	private void getKeys ( Set<String> set, String prefix, JSONObject top )
	{
		final Set<?> thisObj = top.keySet ();
		for ( Object subKeyObj : thisObj )
		{
			final String subKey = subKeyObj.toString ();
			final String topLevelKey = prefix.length () == 0 ? subKey : prefix + "." + subKey;
			set.add ( topLevelKey );
	
			Object subObj = top.get ( subKey );
			if ( subObj instanceof JSONObject )
			{
				getKeys ( set, topLevelKey, (JSONObject) subObj );
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T getGenericValue ( String key, T sampleValue ) throws MissingReqdSettingException
	{
		try
		{
			final Object o = JsonEval.eval ( fObject, key );
			if ( o != null )
			{
				if ( sampleValue.getClass ( ).isAssignableFrom ( o.getClass () ) )
				{
					return (T) o;
				}
			}
		}
		catch ( JSONException e )
		{
			// ignore
		}
		throw new MissingReqdSettingException ( key );
	}

	private <T> T optGenericValue ( String key, T defVal )
	{
		try
		{
			return getGenericValue ( key, defVal );
		}
		catch ( MissingReqdSettingException e )
		{
			return defVal;
		}
	}
}
