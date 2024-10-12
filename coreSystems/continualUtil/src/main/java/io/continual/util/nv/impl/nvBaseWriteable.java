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

import io.continual.util.data.TypeConvertor;

import io.continual.util.nv.NvWriteable;

public abstract class nvBaseWriteable extends nvBaseReadable implements NvWriteable
{
	@Override
	public void set ( String key, char value )
	{
		set ( key, "" + value );
	}

	@Override
	public void set ( String key, boolean value )
	{
		set ( key, Boolean.valueOf ( value ).toString () );
	}

	@Override
	public void set ( String key, int value )
	{
		set ( key, Integer.valueOf ( value ).toString () );
	}

	@Override
	public void set ( String key, long value )
	{
		set ( key, Long.valueOf ( value ).toString () );
	}

	@Override
	public void set ( String key, double value )
	{
		set ( key, Double.valueOf ( value ).toString () );
	}

	@Override
	public void set ( String key, byte[] value )
	{
		set ( key, TypeConvertor.bytesToHex ( value ) );
	}

	@Override
	public void set ( String key, byte[] value, int offset, int length )
	{
		set ( key, TypeConvertor.bytesToHex ( value, offset, length ) );
	}

	@Override
	public void set ( String key, String[] values )
	{
		final StringBuffer sb = new StringBuffer ();
		boolean some = false;
		for ( String val : values )
		{
			if ( some ) sb.append ( "," );
			sb.append ( val );
			some = true;
		}
		set ( key, sb.toString () );
	}

	@Override
	public void set ( Map<String, String> map )
	{
		for ( Map.Entry<String, String> e : map.entrySet () )
		{
			set ( e.getKey (), e.getValue () );
		}
	}
}
