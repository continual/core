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

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.resources.ResourceLoader;
import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.DaemonConsole;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;
import io.continual.util.nv.NvWriteable;

public class Server<T extends ServiceContainer> extends DaemonConsole
{
	public static final String kServices = "services";
	public static final String kServicesChar = "s";

	public static final String kProfile = "profile";
	public static final String kProfileChar = "p";

	protected Server ( String programName, ServiceContainerFactory<T> factory )
	{
		super ( programName );

		fFactory = factory;
		fServices = null;
	}

	@Override
	protected Server<T> setupDefaults ( NvWriteable pt )
	{
		pt.set ( kServices, "services.json" );
		return this;
	}

	@Override
	protected Server<T> setupOptions ( CmdLineParser p )
	{
		super.setupOptions ( p );

		p.registerOptionWithValue ( kServices, kServicesChar, null, null );
		p.registerOptionWithValue ( kProfile, kProfileChar, null, null );

		return this;
	}

	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs clp ) throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException 
	{
		super.quietStartup ();
		for ( String line : kGreeting1 )
		{
			log.info ( line.replaceAll ( "PROGNAME", getProgramName () ) );
		}
		if ( !fHideCopyrights )
		{
			for ( String notice : getCopyrightLines () )
			{
				log.info ( notice );
			}
		}
		for ( String line : kGreeting2 )
		{
			log.info ( line.replaceAll ( "PROGNAME", getProgramName () ) );
		}

		final Looper result = super.init ( p, clp );

		final String services = p.get ( kServices );
		if ( services == null )
		{
			log.warn ( "No services configuration name provided." );
		}
		else
		{
			log.info ( "Loading services from [" + services + "]..." );

			try ( final InputStream serviceStream = ResourceLoader.load ( services ) )
			{
				if ( serviceStream != null )
				{
					fServices = ServiceContainer.build (
						serviceStream,
						p.getStrings ( kProfile, new String[]{"default"} ),
						true,
						fFactory
					);
				}
				else
				{
					log.warn ( "Couldn't find resource " + services + "." );
				}
			}
			catch ( IOException e )
			{
				log.warn ( "Couldn't open " + services + " as a stream." );
			}

			if ( fServices == null )
			{
				log.warn ( "No services loaded from " + services );
			}
		}
		
		return result;
	}

	@Override
	protected boolean daemonStillRunning ()
	{
		if ( fServices != null )
		{
			for ( Service sc : fServices.getServices () )
			{
				if ( sc.isRunning () ) return true;
			}
		}
		log.info ( "No services running." );
		return false;
	}

	protected void hideCopyrights ()
	{
		fHideCopyrights = true;
	}

	public static void runServer ( String programName, String args[] ) throws Exception
	{
		runServer ( programName, new StdFactory (), args );
	}

	public static <T extends ServiceContainer> void runServer ( String programName, ServiceContainerFactory<T> factory, String args[] ) throws Exception
	{
		new Server<> ( programName, factory )
			.runFromMain ( args )
		;
	}

	private final ServiceContainerFactory<T> fFactory;
	private ServiceContainer fServices;
	private boolean fHideCopyrights = false;

	private static final Logger log = LoggerFactory.getLogger ( Server.class );

	private static final String[] kGreeting1 =
		new String[]
		{
				". . . . . . . . . . . . . . . . . . . . . . . . . . .",
				"PROGNAME",
		};
	private static final String[] kGreeting2 =
		new String[]
		{
			". . . . . . . . . . . . . . . . . . . . . . . . . . .",
		};

	public static class StdFactory implements ServiceContainerFactory<ServiceContainer>
	{
		@Override
		public ServiceContainer create ()
		{
			return new ServiceContainer ();
		}
	}
}
