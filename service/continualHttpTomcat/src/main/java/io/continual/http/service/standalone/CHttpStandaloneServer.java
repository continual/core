/*
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

package io.continual.http.service.standalone;

import java.io.File;
import java.util.logging.Logger;

import javax.servlet.Servlet;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.DaemonConsole;

import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvWriteable;

/**
 * A standalone HTTP server built with embedded Tomcat. To use this class, derive a class,
 * instantiate it, and call "runFromMain".
 *
 */
public abstract class CHttpStandaloneServer extends DaemonConsole
{
	public static final String kSetting_Port = "port";
	public static final int kDefault_Port = 8080;

	public static final String kSetting_WebRoot = "webroot";

	/**
	 * Set the defaults for the server.
	 */
	@Override
	protected CHttpStandaloneServer setupDefaults ( NvWriteable pt )
	{
		pt.set ( kSetting_WebRoot, "." );
		return this;
	}

	/**
	 * Setup the options for the base class console system.
	 */
	@Override
	protected CHttpStandaloneServer setupOptions ( CmdLineParser p )
	{
		super.setupOptions ( p );

		p.registerOptionWithValue ( kSetting_WebRoot );
		p.registerOptionWithValue ( kSetting_Port, "p", null, null );

		return this;
	}

	/**
	 * Setup and run the embedded Tomcat server.
	 */
	@Override
	protected Looper init ( NvReadable p, CmdLinePrefs clp ) throws NvReadable.MissingReqdSettingException, NvReadable.InvalidSettingValueException, StartupFailureException
	{
		// do setup, then return the base class init()
		
		fCommandLineSettings = p;

		final Looper result = super.init ( p, clp );

		fTomcat = new Tomcat ();

		final int port = p.getInt ( kSetting_Port, kDefault_Port );
		fTomcat.setPort ( port );

		final File base = new File ( System.getProperty ( "java.io.tmpdir" ) );

		final Context rootCtx = fTomcat.addContext ( "", base.getAbsolutePath () );

		// wire in servlet
		final String servletName = getProgramName();
		Tomcat.addServlet ( rootCtx, servletName, createServlet ( p ) );
		rootCtx.addServletMappingDecoded ( "/*", servletName );

		try
		{
			fTomcat.start();
		}
		catch ( LifecycleException e )
		{
			throw new StartupFailureException ( e );
		}

		log.info ( "Server listening on port " + port + "." );
		
		return result;
	}

	protected abstract Servlet createServlet ( NvReadable p );
	
	protected CHttpStandaloneServer ( String programName )
	{
		super ( programName );

		fCommandLineSettings = null;
	}

	protected NvReadable getCommandLineSettings ()
	{
		return fCommandLineSettings;
	}

	/**
	 * There's no mechanism for cleanly shutting down this program.
	 */
	@Override
	protected boolean daemonStillRunning ()
	{
		fTomcat.getServer ().await ();
		return false;
	}

	private Tomcat fTomcat;
	private NvReadable fCommandLineSettings;

	private static final Logger log = Logger.getLogger ( CHttpStandaloneServer.class.getName () );
}
