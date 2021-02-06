package io.continual.basesvcs.services.http;

import java.io.File;
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

import io.continual.basesvcs.services.accounts.AccountService;
import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.DrumlinServlet.SessionLifeCycle;
import io.continual.services.Service;
import io.continual.services.ServiceContainer;

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
				fAccounts = fServices.get ( acctsServiceName, AccountService.class );
			}
			else
			{
				fAccounts = null;
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
				final Connector connector = new Connector ( Http11NioProtocol.class.getName () );
				connector.setPort ( port );
				fTomcat.getService ().addConnector ( connector );
	
				log.info ( "Service [" + fSettings.optString ( "name", "<anonymous>" ) + "] listens for HTTP on " + port + "." );
			}
	
			// HTTPS setup...
			if ( httpsConfig != null )
			{
				final int port = httpsConfig.optInt ( kSetting_Port, defaultPort );
	
				final Connector connector = new Connector ( Http11NioProtocol.class.getName () );

				final JSONObject keystore = httpsConfig.getJSONObject ( kSetting_Keystore );

				String keystoreFilename = keystore.getString ( kSetting_KeystoreFile );
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
	
				connector.setScheme ( "https" );
				connector.setSecure ( true );
				connector.setAttribute ( "keystoreFile", keystoreFilename );
				connector.setAttribute ( "keystorePass", keystore.optString ( kSetting_KeystorePassword, kDefault_KeystorePassword ) );
				connector.setAttribute ( "keyAlias", keystore.optString ( kSetting_KeystoreAlias, kDefault_KeystoreAlias ) );
				connector.setAttribute ( "clientAuth", "false" );
				connector.setAttribute ( "sslProtocol", "TLS" );
				connector.setAttribute ( "SSLEnabled", true );
				connector.setPort ( port );
		
				fTomcat.getService ().addConnector ( connector );
	
				log.info ( "Service [" + fSettings.optString ( "name", "<anonymous>" ) + "] listens for HTTPS on " + port + "." );
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
	public void start ()
	{
		if ( fSettings.optBoolean ( "enabled", true ) )
		{
			final HttpServlet hs = new HttpServlet (
				fServices,
				fAccounts,
				SessionLifeCycle.valueOf ( fSettings.optString ( "lifeCycle", SessionLifeCycle.NO_SESSION.toString () ) )
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
	private final AccountService<?,?> fAccounts;

	private final JSONObject fSettings;
	private final Tomcat fTomcat;
	private boolean fRunning;
	private final HashMap<String,HttpRouter> fServlets;
	private final File fWorkDir;

	private static final Logger log = LoggerFactory.getLogger ( HttpService.class );
}
