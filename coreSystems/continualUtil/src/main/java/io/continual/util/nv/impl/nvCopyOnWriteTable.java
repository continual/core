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
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.continual.util.data.TypeConvertor;

import io.continual.util.nv.NvWriteable;

public class nvCopyOnWriteTable extends nvBaseReadable implements NvWriteable
{
	public nvCopyOnWriteTable ()
	{
		fData = new data ();
		fData.attach ();
	}

	public nvCopyOnWriteTable ( nvCopyOnWriteTable that )
	{
		fData = that.fData;
		fData.attach ();
	}

	public nvCopyOnWriteTable ( Map<String,String> values )
	{
		this ();
		fData.putAll ( values );
	}

	@Override
	public synchronized boolean hasValueFor ( String key )
	{
		return fData.containsKey ( key );
	}

	@Override
	public synchronized String getString ( String key ) throws MissingReqdSettingException
	{
		return fData.get ( key );
	}

	@Override
	public String[] getStrings ( String key ) throws MissingReqdSettingException
	{
		final String fullset = getString ( key );
		return fullset.split ( ",", -1 );
	}

	public synchronized Map<String,String> getCopyAsMap ()
	{
		final HashMap<String,String> result = new HashMap<String,String> ();
		result.putAll ( fData.getSharedMap () );
		return result;
	}

	public synchronized int size ()
	{
		return fData.size ();
	}

	public synchronized Collection<String> getAllKeys ()
	{
		return fData.getAllKeys ();
	}

	@Override
	public synchronized void copyInto ( NvWriteable writeable )
	{
		fData.copyInto ( writeable );
	}

	@Override
	public synchronized void copyInto ( Map<String,String> that )
	{
		fData.copyInto ( that );
	}

	private data fData;

	synchronized data getDataReference ()
	{
		return fData;
	}

	@Override
	public synchronized void clear ()
	{
		fData = fData.clear ();
	}

	@Override
	public synchronized void set ( String key, String value )
	{
		if ( value != null )
		{
			fData = fData.put ( key, value );
		}
	}

	@Override
	public synchronized void unset ( String key )
	{
		fData = fData.remove ( key );
	}

	@Override
	public synchronized void set ( Map<String, String> map )
	{
		fData = fData.putAll ( map );
	}

	@Override
	public void set ( String key, int value )
	{
		set ( key, "" + value );
	}

	@Override
	public void set ( String key, long value )
	{
		set ( key, "" + value );
	}

	@Override
	public void set ( String key, double value )
	{
		set ( key, "" + value );
	}

	@Override
	public void set ( String key, boolean value )
	{
		set ( key, "" + value );
	}

	@Override
	public void set ( String key, char value )
	{
		set ( key, "" + value );
	}

	@Override
	public void set ( String key, byte[] value )
	{
		set ( key, value, 0, value.length );
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
		boolean one = false;
		for ( String value : values )
		{
			if ( one ) sb.append ( "," );
			sb.append ( value );
			one = true;
		}
		set ( key, sb.toString () );
	}

	class data
	{
		public data ()
		{
			fRefs = 0;
			fData = new Hashtable<String,String> ();
			fWeight = 0;
		}

		public synchronized String get ( String key )
		{
			return fData.get ( key );
		}

		public synchronized data put ( String key, String value )
		{
			final data d = prepForWrite ();
			d.fData.put ( key, value );
			d.fWeight += calcWeight ( key, value );
			return d;
		}

		public synchronized void attach ()
		{
			fRefs++;
		}

		public synchronized void detach ()
		{
			fRefs--;
		}

		public synchronized Set<Entry<String, String>> entrySet ()
		{
			return fData.entrySet ();
		}

		public synchronized int size ()
		{
			return fData.size ();
		}

		public synchronized Collection<String> getAllKeys ()
		{
			return fData.keySet ();
		}

		public synchronized data clear ()
		{
			final data d = prepForWrite ();
			d.fData.clear ();
			d.fWeight = 0;
			return d;
		}

		public synchronized data remove ( String key )
		{
			final data d = prepForWrite ();
			final String val = d.fData.get ( key );
			d.fData.remove ( key );
			d.fWeight -= calcWeight ( key, val );
			return d;
		}

		public synchronized data putAll ( Map<String, String> map )
		{
			final data d = prepForWrite ();
			d.fData.putAll ( map );
			for ( Map.Entry<String, String> e : map.entrySet () )
			{
				d.fWeight += calcWeight ( e.getKey(), e.getValue () );
			}
			return d;
		}

		public synchronized boolean containsKey ( String key )
		{
			return fData.containsKey ( key );
		}

		public synchronized void copyInto ( NvWriteable writeable )
		{
			writeable.set ( fData );
		}

		public synchronized void copyInto ( Map<String, String> that )
		{
			that.putAll ( fData );
		}

		synchronized int getRefCount ()
		{
			return fRefs;
		}

		synchronized Map<String,String> getSharedMap ()
		{
			return fData;
		}

		synchronized double estimateWeight()
		{
			return (double) fWeight / (double) fRefs;
		}

		private int fRefs;
		private Hashtable<String,String> fData;
		private long fWeight;

		private data prepForWrite ()
		{
			data result = this;
			if ( fRefs > 1 )
			{
				result = new data ();
				result.attach ();
				result.fData.putAll ( fData );
				result.fWeight = fWeight;
				detach ();
			}
			return result;
		}

		long calcWeight ( String key, String val )
		{
			return key.length () + ( val == null ? 0 : val.length () );
		}
	}
}
