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

import java.util.Map;
import java.util.Map.Entry;

import io.continual.util.data.TypeConvertor;
import io.continual.util.data.StringUtils;
import io.continual.util.data.TypeConvertor.conversionError;

import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvWriteable;

public abstract class nvBaseReadable implements NvReadable
{
	public abstract boolean hasValueFor ( String key );

	public abstract String getString ( String key ) throws MissingReqdSettingException;

	protected nvBaseReadable ()
	{
	}

	@Override
	public String toString ()
	{
		final StringBuffer sb = new StringBuffer ();
		for ( String key : getAllKeys () )
		{
			if ( sb.length () > 0 )
			{
				sb.append ( ", " );
			}
			final String val = getString ( key, "??" );
			sb.append ( key ).append ( ":" ).append ( val );
		}
		return sb.toString ();
	}

	@Override
	public String get ( String key )
	{
		return getString ( key, null );
	}

	@Override
	public String getString ( String key, String defValue )
	{
		try
		{
			return getString ( key );
		}
		catch ( MissingReqdSettingException e )
		{
			return defValue;
		}
	}

	@Override
	public boolean getBoolean ( String key ) throws MissingReqdSettingException
	{
		return TypeConvertor.convertToBoolean ( getString ( key ) );
	}

	@Override
	public boolean getBoolean ( String key, boolean defValue )
	{
		try
		{
			return getBoolean ( key );
		}
		catch ( MissingReqdSettingException e )
		{
			return defValue;
		}
	}

	@Override
	public int getInt ( String key ) throws MissingReqdSettingException
	{
		try
		{
			return TypeConvertor.convertToInt ( getString ( key ) );
		}
		catch ( conversionError e )
		{
			throw new MissingReqdSettingException ( key, e );
		}
	}

	@Override
	public int getInt ( String key, int defValue )
	{
		try
		{
			return getInt ( key );
		}
		catch ( MissingReqdSettingException e )
		{
			return defValue;
		}
	}

	@Override
	public double getDouble ( String key ) throws MissingReqdSettingException
	{
		try
		{
			return TypeConvertor.convertToDouble ( getString ( key ) );
		}
		catch ( conversionError e )
		{
			throw new MissingReqdSettingException ( key, e );
		}
	}

	@Override
	public double getDouble ( String key, double defValue )
	{
		try
		{
			return getDouble ( key );
		}
		catch ( MissingReqdSettingException e )
		{
			return defValue;
		}
	}

	public abstract String[] getStrings ( String key ) throws MissingReqdSettingException;

	public String[] getStrings ( String key, String[] defValue )
	{
		try
		{
			return getStrings ( key );
		}
		catch ( MissingReqdSettingException e )
		{
			return defValue;
		}
	}

	@Override
	public char getCharacter ( String key ) throws MissingReqdSettingException
	{
		try
		{
			return TypeConvertor.convertToCharacter ( getString ( key ) );
		}
		catch ( conversionError e )
		{
			throw new MissingReqdSettingException ( key, e );
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
	public long getLong ( String key ) throws MissingReqdSettingException
	{
		try
		{
			return TypeConvertor.convertToLong ( getString ( key ) );
		}
		catch ( conversionError e )
		{
			throw new MissingReqdSettingException ( key, e );
		}
	}

	@Override
	public long getLong ( String key, long defValue )
	{
		try
		{
			return getLong ( key );
		}
		catch ( MissingReqdSettingException e )
		{
			return defValue;
		}
	}

	@Override
	public byte[] getBytes ( String key ) throws MissingReqdSettingException, InvalidSettingValueException
	{
		try
		{
			return TypeConvertor.hexToBytes ( getString ( key ) );
		}
		catch ( conversionError e )
		{
			throw new InvalidSettingValueException ( key, e );
		}
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
	public void copyInto ( NvWriteable writeable )
	{
		for ( Entry<String, String> e : getCopyAsMap ().entrySet () )
		{
			writeable.set ( e.getKey(), e.getValue () );
		}
	}

	@Override
	public void copyInto ( Map<String, String> writeable )
	{
		for ( Entry<String, String> e : getCopyAsMap ().entrySet () )
		{
			writeable.put ( e.getKey(), e.getValue () );
		}
	}

	@Override
	public void rescan () throws LoadException
	{
	}

	protected String eval ( String val )
	{
		return StringUtils.evaluate ( this, val );
	}
}
