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
import java.util.Map;
import java.util.TreeSet;

import org.slf4j.LoggerFactory;

import io.continual.util.nv.NvReadable;

/**
 * This class acts as a wrapper around the basic rrNvReadable settings class, and it
 * provides the ability to make settings specific to an "installation type" (e.g.
 * debug, test, production).
 */
public class nvInstallTypeWrapper extends nvBaseReadable
{
	public nvInstallTypeWrapper ( NvReadable actual )
	{
		fActual = actual;
		fKeys = new TreeSet<String> ();

		fThisUser = System.getProperty ( "user.name" );

		fSystemType = System.getProperty ( "rr.installation", null );
		if ( fSystemType != null )
		{
			LoggerFactory.getLogger ( nvInstallTypeWrapper.class ).info ( "rr.installation: " + fSystemType );
		}

		parseForKeys ();
	}

	@Override
	public int size ()
	{
		return fKeys.size ();
	}

	@Override
	public Collection<String> getAllKeys ()
	{
		return fKeys;
	}

	@Override
	public Map<String, String> getCopyAsMap ()
	{
		final HashMap<String,String> map = new HashMap<String,String> ();
		for ( String key : fKeys )
		{
			map.put ( key, getString ( key, "" ) );
		}
		return map;
	}

	@Override
	public boolean hasValueFor ( String key )
	{
		return fKeys.contains ( key );
	}

	@Override
	public String getString ( String key ) throws MissingReqdSettingException
	{
		String result = null;

		// try keys from most specific to least

		if ( fSystemType != null && fThisUser != null )
		{
			final String keyToTry = ( key + "[" + fSystemType + "@" + fThisUser + "]" );
			result = fActual.getString ( keyToTry, null );
		}
		
		if ( result == null && fSystemType != null )
		{
			final String keyToTry = ( key + "[" + fSystemType + "]" );
			result = fActual.getString ( keyToTry, null );
		}

		if ( result == null && fThisUser != null )
		{
			final String keyToTry = ( key + "[@" + fThisUser + "]" );
			result = fActual.getString ( keyToTry, null );
		}

		if ( result == null )
		{
			result = fActual.getString ( key );
		}

		return result;
	}

	@Override
	public String[] getStrings ( String key ) throws MissingReqdSettingException
	{
		String[] result = null;

		// try keys from most specific to least

		if ( fSystemType != null && fThisUser != null )
		{
			final String keyToTry = ( key + "[" + fSystemType + "@" + fThisUser + "]" );
			result = fActual.getStrings ( keyToTry, null );
		}
		
		if ( result == null && fSystemType != null )
		{
			final String keyToTry = ( key + "[" + fSystemType + "]" );
			result = fActual.getStrings ( keyToTry, null );
		}

		if ( result == null && fThisUser != null )
		{
			final String keyToTry = ( key + "[@" + fThisUser + "]" );
			result = fActual.getStrings ( keyToTry, null );
		}

		if ( result == null )
		{
			result = fActual.getStrings ( key );
		}

		return result;
	}

	@Override
	public void rescan () throws LoadException
	{
		super.rescan ();
		parseForKeys ();
	}

	private final NvReadable fActual;
	private final TreeSet<String> fKeys;
	private final String fSystemType;
	private final String fThisUser;

	private void parseForKeys ()
	{
		fKeys.clear ();
		for ( String key : fActual.getAllKeys () )
		{
			fKeys.add ( parse ( key ) );
		}
	}

	static String parse ( String key )
	{
		// format:
		//		plain
		//		plain[sysType]
		//		plain[@user]
		//		plain[sysType@user]
		key = key.trim ();
		if ( key.matches ( ".*\\[[^\\[\\]]+\\]" ) )
		{
			final int openBracket = key.indexOf ( '[' );
			key = key.substring ( 0, openBracket );
		}
		return key;
	}
}
