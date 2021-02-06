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

import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvWriteable;

public class nvWriteableTable extends nvReadableTable implements NvWriteable
{
	public nvWriteableTable ()
	{
		super ();
	}

	public nvWriteableTable ( Map<String,String> content )
	{
		super ( content );
	}

	public nvWriteableTable ( NvReadable that )
	{
		super ( that == null ? null : that.getCopyAsMap () );
		if ( that != null )
		{
			for ( String key : that.getAllKeys () )
			{
				set ( key, that.getString ( key, null ) );
			}
		}
	}

	@Override
	public synchronized void set ( String key, String value )
	{
		super.set ( key, value );
	}

	@Override
	public void set ( String key, char value )
	{
		super.set ( key, "" + value );
	}

	public synchronized void set ( String key, int value )
	{
		set ( key, "" + value );
	}

	public synchronized void set ( String key, long value )
	{
		set ( key, "" + value );
	}

	public synchronized void set ( String key, double value )
	{
		set ( key, "" + value );
	}

	public synchronized void set ( String key, boolean value )
	{
		set ( key, "" + value );
	}

	public synchronized void set ( Map<String,String> map )
	{
		super.set ( map );
	}

	@Override
	public synchronized void unset ( String key )
	{
		super.clear ( key );
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
		final StringBuffer sb = new StringBuffer ();
		boolean one = false;
		for ( String value : values )
		{
			if ( one ) sb.append ( "," );
			sb.append ( value );
			one = true;
		}
		set ( key, sb.toString () );
	}
}
