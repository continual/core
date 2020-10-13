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

package io.continual.restHttp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.CHttpServlet.SessionLifeCycle;
import io.continual.iam.IamService;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.MetricsService;
import io.continual.services.Service;
import io.continual.services.ServiceContainer;
import io.continual.util.data.StreamTools;

public class HttpService implements Service
{
	private final String kSetting_ServletWorkDir = "workDir";
	private final String kDefault_ServletWorkDir = new File ( System.getProperty ( "java.io.tmpdir" )).getAbsolutePath ();

	private final String kSetting_Keystore = "keystore";
	
	private final String kSetting_KeystoreFile = "file";

	private final String kSetting_KeystoreAlias = "alias";
	private final String kDefault_KeystoreAlias = "tomcat";

	private final String kSetting_KeystorePassword = "password";
	private final String kDefault_KeystorePassword = "changeme";

	private final String kSetting_KeystorePasswordFile = "passwordFile";
	
	private final String kSetting_Port = "port";

	public HttpService ( ServiceContainer sc, JSONObject settings ) throws BuildFailure
	{
		this (
			sc,
			settings,
			settings.optString ( "name", "default" ),
			settings.optInt ( "port", 8080 ) 
		);
	}

	protected HttpService ( ServiceContainer sc, JSONObject settings, String serviceName, int defaultPort ) throws BuildFailure
	{
		try
		{
			fServices = sc;

			final String acctsServiceName = settings.optString ( "accountService", null );
			if ( acctsServiceName != null )
			{
				fAccounts = fServices.get ( acctsServiceName, IamService.class );
			}
			else
			{
				fAccounts = null;
			}

			final String metricsServiceName = settings.optString ( "metricsService", null );
			if ( metricsServiceName != null )
			{
				final MetricsService ms = fServices.get ( metricsServiceName, MetricsService.class );
				if ( ms == null ) throw new BuildFailure ( "Metrics service specified as " + metricsServiceName + " but the service was not found." );

				fMetrics = ms.getCatalog ( "http" );
			}
			else
			{
				fMetrics = null;
			}

			fSettings = settings;
			fRunning = false;
	
			fServlets = new HashMap<String,HttpRouter> ();
	
			System.setProperty (
				"org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH",
				"true" );
			fTomcat = new Tomcat ();
	
			final String servletWorkDir = settings.optString ( kSetting_ServletWorkDir, kDefault_ServletWorkDir );
			fWorkDir = new File ( servletWorkDir );
			if ( !fWorkDir.exists () )
			{
				fWorkDir.mkdirs ();
			}
	
			// the settings can have sub-objects "http" and/or "https". If neither is present, read "port" for the port number.
			JSONObject httpConfig = settings.optJSONObject ( "http" );
			final JSONObject httpsConfig = settings.optJSONObject ( "https" );
	
			if ( httpConfig == null && httpsConfig == null )
			{
				httpConfig = new JSONObject ()
					.put ( "port", settings.optInt ( kSetting_Port, defaultPort ) )
				;
			}
	
			// HTTP setup...
			if ( httpConfig != null )
			{
				final int port = httpConfig.optInt ( kSetting_Port, defaultPort );
				if ( port > 0 )
				{
					final Connector connector = new Connector ( Http11NioProtocol.class.getName () );
					connector.setPort ( port );
					fTomcat.getService ().addConnector ( connector );
		
					log.info ( "Service [" + fSettings.optString ( "name", "<anonymous>" ) + "] listens for HTTP on " + port + "." );
				}
				else
				{
					log.info ( "Service [" + fSettings.optString ( "name", "<anonymous>" ) + "] will not listen for HTTP." );
				}
			}
	
			// HTTPS setup...
			if ( httpsConfig != null )
			{
				final int port = httpsConfig.optInt ( kSetting_Port, defaultPort );
				if ( port > 0 )
				{
					final Connector connector = new Connector ( Http11NioProtocol.class.getName () );
	
					final JSONObject keystoreConfig = httpsConfig.getJSONObject ( kSetting_Keystore );
	
					String keystoreFilename = keystoreConfig.getString ( kSetting_KeystoreFile );
					final File keystoreFile = new File ( keystoreFilename );
					if ( !keystoreFile.isAbsolute () )
					{
						// tomcat requires an absolute filename, or it'll use env var CATALINA_HOME. We want a file
						// relative to the current working directory.
						final Path path = FileSystems.getDefault().getPath(".").toAbsolutePath();
						final File file = new File ( path.toFile (), keystoreFilename );
						keystoreFilename = file.getAbsolutePath ();
						log.info ( "Using absolute path [" + keystoreFilename + "] for keystore." );
					}

					// the keystore password can be delivered directly in configuration, or loaded from a file
					String keystorePassword = "";
					if ( keystoreConfig.has ( kSetting_KeystorePassword ) )
					{
						keystorePassword = keystoreConfig.optString ( kSetting_KeystorePassword, kDefault_KeystorePassword );
					}
					else if ( keystoreConfig.has ( kSetting_KeystorePasswordFile ) )
					{
						final String pwdFileName = keystoreConfig.optString ( kSetting_KeystorePasswordFile, null );
						final File pwdFile = new File ( pwdFileName );
						try ( FileInputStream fis = new FileInputStream ( pwdFile ) )
						{
							final byte[] pwdData = StreamTools.readBytes ( fis );
							keystorePassword = new String ( pwdData );
						}
						catch ( IOException x )
						{
							log.warn ( "There was a problem trying to read {}: {}", pwdFileName, x.getMessage () );
						}
					}
					
					connector.setScheme ( "https" );
					connector.setSecure ( true );
					connector.setAttribute ( "keystoreFile", keystoreFilename );
					connector.setAttribute ( "keystorePass", keystorePassword );
					connector.setAttribute ( "keyAlias", keystoreConfig.optString ( kSetting_KeystoreAlias, kDefault_KeystoreAlias ) );
					connector.setAttribute ( "clientAuth", "false" );
					connector.setAttribute ( "sslProtocol", "TLS" );
					connector.setAttribute ( "SSLEnabled", true );
					connector.setPort ( port );
			
					fTomcat.getService ().addConnector ( connector );
		
					log.info ( "Service [" + fSettings.optString ( "name", "<anonymous>" ) + "] listens for HTTPS on " + port + "." );
				}
				else
				{
					log.info ( "Service [" + fSettings.optString ( "name", "<anonymous>" ) + "] will not listen for HTTPS." );
				}
			}
		}
		catch ( JSONException x )
		{
			throw new BuildFailure ( x );
		}
	}

	public HttpService addRouter ( String servletName, HttpRouter s )
	{
		fServlets.put ( servletName, s );
		return this;
	}

	@Override
	public void start () throws FailedToStart
	{
		try
		{
			if ( fSettings.optBoolean ( "enabled", true ) )
			{
				final HttpServlet hs = new HttpServlet (
					fServices,
					fAccounts,
					SessionLifeCycle.valueOf ( fSettings.optString ( "lifeCycle", SessionLifeCycle.NO_SESSION.toString () ) ),
					fSettings,
					fMetrics
				);
				for ( Entry<String, HttpRouter> s : fServlets.entrySet () )
				{
					hs.addRouter ( s.getValue () );
				}
	
				final String servletName = "httpService";
				final Context rootCtx = fTomcat.addContext ( "", fWorkDir.getAbsolutePath () );
				Tomcat.addServlet ( rootCtx, servletName, hs );
				rootCtx.addServletMappingDecoded ( "/*", servletName );
	
				try
				{
					fTomcat.start();
					fRunning = true;
				}
				catch ( LifecycleException e )
				{
					log.warn ( "Couldn't start tomcat." , e );
					fRunning = false;
				}
		
				log.info ( "Service [" + fSettings.optString ( "name", "<anonymous>" ) + "] is listening." );
			}
			else
			{
				log.info ( "Service [" + fSettings.optString ( "name", "<anonymous>" ) + "] is disabled." );
			}
		}
		catch ( BuildFailure x )
		{
			throw new FailedToStart ( x );
		}
	}

	@Override
	public void requestFinish ()
	{
		try
		{
			fTomcat.stop ();
			fRunning = false;
		}
		catch ( LifecycleException e )
		{
			log.warn ( "Couldn't stop tomcat.", e );
		}
	}

	@Override
	public boolean isRunning ()
	{
		return fRunning;
	}

	private final ServiceContainer fServices;
	private final IamService<?,?> fAccounts;
	private final MetricsCatalog fMetrics;

	private final JSONObject fSettings;
	private final Tomcat fTomcat;
	private boolean fRunning;
	private final HashMap<String,HttpRouter> fServlets;
	private final File fWorkDir;

	private static final Logger log = LoggerFactory.getLogger ( HttpService.class );
}
