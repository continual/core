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

import java.io.File;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvWriteable;
import io.continual.util.nv.impl.nvPropertiesFile;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.LoadException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

/**
 * A console that uses c/config to load a config file
 */
public abstract class ConfiguredConsole extends ConsoleProgram
{
	public static final String kConfigFile = "config";
	public static final String kConfigFileChar = "c";

	protected ConfiguredConsole ()
	{
		this ( null );
	}
	
	protected ConfiguredConsole ( String shortName )
	{
		fShortName = shortName;
	}

	@Override
	protected abstract Looper init ( NvReadable p, CmdLinePrefs clp ) throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException;

	@Override
	protected ConfiguredConsole setupDefaults ( NvWriteable pt )
	{
		return this;
	}

	@Override
	protected ConfiguredConsole setupOptions ( CmdLineParser p )
	{
		super.setupOptions ( p );

		p.registerOptionWithValue ( kConfigFile, kConfigFileChar, null, null );
		
		return this;
	}

	protected NvReadable loadFile ( String name ) throws LoadException
	{
		NvReadable result = null;
		final File cf = new File ( name );
		if ( cf.exists () )
		{
			result = new nvPropertiesFile ( cf );
		}
		else
		{
			log.warn ( "Couldn't load configuration file [" + name + "]." );
		}
		return result;
	}

	@Override
	protected NvReadable loadAdditionalConfig ( NvReadable currentPrefs ) throws LoadException
	{
		// try three names, in order:
		//		whatever is configured
		//		the short name + ".properties" in the classpath
		//		"etc/" + short name + ".properties" as a file

		final String cfn = currentPrefs.getString ( kConfigFile, null );
		if ( cfn != null )
		{
			return loadFile ( cfn );
		}
		else if ( fShortName != null )
		{
			String props = fShortName + ".properties";
			URL url = ConfiguredConsole.class.getClassLoader ().getResource ( props );
			if ( url != null )
			{
				return new nvPropertiesFile ( url );
			}

			props = "etc/" + props;
			return loadFile ( props );
		}
		return null;
	}

	private final String fShortName;

	private static final Logger log = LoggerFactory.getLogger ( ConfiguredConsole.class );
}
