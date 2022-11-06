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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Logger;

public class nvPropertiesFile extends nvBaseReadable
{
	/**
	 * A reusable stream fetcher
	 */
	public interface StreamFetcher
	{
		InputStream getStream () throws LoadException;
	}

	public nvPropertiesFile ( final File f ) throws LoadException
	{
		this ( new StreamFetcher ()
		{
			@Override public String toString() { return f.toString(); }

			@Override
			public InputStream getStream () throws LoadException
			{
				try
				{
					return new FileInputStream ( f );
				}
				catch ( FileNotFoundException e )
				{
					throw new LoadException ( e );
				}
			}
		} );
	}

	public nvPropertiesFile ( final InputStream is ) throws LoadException
	{
		this ( new StreamFetcher ()
		{
			@Override public String toString() { return "input stream"; }

			@Override
			public InputStream getStream () throws LoadException
			{
				return is;
			}
		} );
	}

	public nvPropertiesFile ( final URL u ) throws LoadException
	{
		this ( new StreamFetcher ()
		{
			@Override public String toString() { return u.toString(); }

			@Override
			public InputStream getStream () throws LoadException
			{
				try
				{
					return u.openStream ();
				}
				catch ( IOException e )
				{
					throw new LoadException ( e );
				}
			}
		} );
	}

	public nvPropertiesFile ( StreamFetcher sf ) throws LoadException
	{
		super ();

		fFetcher = sf;
		fPrefs = new Properties ();
		rescan ();
	}

	public String getString ( String key ) throws MissingReqdSettingException
	{
		final String result = fPrefs.getProperty ( key );
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
	public boolean hasValueFor ( String key )
	{
		return fPrefs.containsKey ( key );
	}

	@Override
	public void rescan () throws LoadException
	{
		log.info ( "Rescanning settings " + fFetcher.toString() );
		fPrefs.clear ();
		read ( fFetcher.getStream () );
	}

	@Override
	public int size ()
	{
		return fPrefs.size ();
	}

	@Override
	public Collection<String> getAllKeys ()
	{
		final TreeSet<String> list = new TreeSet<String> ();
		for ( Object o : fPrefs.keySet () )
		{
			list.add ( o.toString () );
		}
		return list;
	}

	@Override
	public Map<String, String> getCopyAsMap ()
	{
		HashMap<String,String> map = new HashMap<String,String> ();
		for ( Entry<Object, Object> e : fPrefs.entrySet () )
		{
			map.put ( e.getKey().toString(), e.getValue().toString () );
		}
		return map;
	}

	private final StreamFetcher fFetcher;
	private final Properties fPrefs;

	private static final Logger log = Logger.getLogger ( nvPropertiesFile.class.getName() );

	private void read ( InputStream is ) throws LoadException
	{
		if ( is == null )
		{
			throw new LoadException ( "No stream provided to nvPropertiesFile.read()" );
		}

		try
		{
			fPrefs.load ( is );
		}
		catch ( IOException e )
		{
			throw new LoadException ( e );
		}
	}
}
