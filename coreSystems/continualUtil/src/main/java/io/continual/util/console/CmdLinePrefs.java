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
package io.continual.util.console;

import java.util.Vector;

import io.continual.util.nv.impl.nvWriteableTable;

/**
 * This class participates in the name/value pair system with a CmdLineParser as an
 * underlying source for settings.
 */
public class CmdLinePrefs extends nvWriteableTable
{
	public CmdLinePrefs ( CmdLineParser clp )
	{
		super ();

		fParser = clp;
		fLeftovers = new Vector<String> ();
	}

	/**
	 * get remaining arguments after the options are read
	 * @return a vector of args
	 */
	public Vector<String> getFileArguments ()
	{
		return fLeftovers;
	}

	public String getFileArgumentsAsString ()
	{
		final StringBuffer sb = new StringBuffer ();
		for ( String s : fLeftovers )
		{
			sb.append ( " " );
			sb.append ( s );
		}
		return sb.toString().trim ();
	}
	
	/**
	 * find out if an option was explicitly set by the caller
	 * @param optionWord the option name
	 * @return true or false
	 */
	public boolean wasExplicitlySet ( String optionWord )
	{
		return super.hasValueFor ( optionWord );
	}

	@Override
	public String getString ( String key ) throws MissingReqdSettingException
	{
		String result = null;
		if ( wasExplicitlySet ( key ) )
		{
			result = super.getString ( key );
		}
		else
		{
			result = fParser.getArgFor ( key );
		}

		if ( result == null )
		{
			throw new MissingReqdSettingException ( key );
		}
		return result;
	}

	private final CmdLineParser fParser;
	private final Vector<String> fLeftovers;

	void addLeftover ( String val )
	{
		fLeftovers.add ( val );
	}
}
