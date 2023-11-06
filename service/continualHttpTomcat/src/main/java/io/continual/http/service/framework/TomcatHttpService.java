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

package io.continual.http.service.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Enumeration;

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
import io.continual.http.service.framework.inspection.CHttpObserverMgr;
import io.continual.iam.IamService;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.MetricsService;
import io.continual.metrics.impl.noop.NoopMetricsCatalog;
import io.continual.services.ServiceContainer;
import io.continual.util.data.StreamTools;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

public class TomcatHttpService extends CHttpService
{
	private final String kSetting_ServletWorkDir = "workDir";
	private final String kDefault_ServletWorkDir = new File ( System.getProperty ( "java.io.tmpdir" )).getAbsolutePath ();

	private final String kSetting_Keystore = "keystore";
	
	private final String kSetting_KeystoreFile = "file";

	private final String kSetting_KeystoreType = "type";
	private final String kDefault_KeystoreType = "JKS";

	private final String kSetting_KeystoreAlias = "alias";
	private final String kDefault_KeystoreAlias = "tomcat";

	private final String kSetting_KeystoreAliasScan = "scanForAlias";

	private final String kSetting_KeystorePassword = "password";
	private final String kDefault_KeystorePassword = "changeme";

	private final String kSetting_KeystorePasswordFile = "passwordFile";
	
	private static final String kSetting_Port = "port";
	private static final int kDefault_HttpPort = 8080;
	private static final int kDefault_HttpsPort = 8443;

	public TomcatHttpService ( ServiceContainer sc, JSONObject rawConfig ) throws BuildFailure
	{
		super ( sc, rawConfig );

		try
		{
			final JSONObject settings = sc.getExprEval ().evaluateJsonObject ( rawConfig );
			fSettings = JsonUtil.clone ( settings );

			fName = settings.optString ( "name", "<anonymous>" );
			fRunning = false;

			fLifeCycle = SessionLifeCycle.valueOf ( settings.optString ( "lifeCycle", SessionLifeCycle.NO_SESSION.toString () ) );

			// the accounts service is optional...
			fAccounts = sc.getReqdIfNotNull ( settings.optString ( "accountService", null ), IamService.class );

			// the metrics service is optional...
			final MetricsService ms = sc.getReqdIfNotNull ( settings.optString ( "metricsService", null ), MetricsService.class );
			fMetrics = ms == null ? new NoopMetricsCatalog () : ms.getCatalog ( "http" );

			// the inspection service is also optional...
			fInspector = sc.getReqdIfNotNull ( settings.optString ( "inspector", null ), CHttpObserverMgr.class );

			System.setProperty ( "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true" );
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
	
			// if we have neither http nor https, stand up http on the default port
			if ( httpConfig == null && httpsConfig == null )
			{
				httpConfig = new JSONObject ()
					.put ( kSetting_Port, settings.optInt ( kSetting_Port, kDefault_HttpPort ) )
				;
			}

			// HTTP setup...
			if ( httpConfig != null )
			{
				final int port = httpConfig.optInt ( kSetting_Port, kDefault_HttpPort );
				if ( port > 0 )
				{
					final Connector connector = new Connector ( Http11NioProtocol.class.getName () );
					connector.setPort ( port );
					transferConnectorAttributes ( connector, httpConfig.optJSONObject ( "tomcat" ) );
					
					fTomcat.getService ().addConnector ( connector );
		
					log.info ( "Service [{}] listens for HTTP on {}.", fName, port );
				}
				else
				{
					log.info ( "Service [{}] will not listen for HTTP.", fName );
				}
			}
	
			// HTTPS setup...
			if ( httpsConfig != null )
			{
				final int port = httpsConfig.optInt ( kSetting_Port, kDefault_HttpsPort );
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
					String keystorePassword = kDefault_KeystorePassword;
					if ( keystoreConfig.has ( kSetting_KeystorePassword ) )
					{
						keystorePassword = keystoreConfig.getString ( kSetting_KeystorePassword );
					}
					else if ( keystoreConfig.has ( kSetting_KeystorePasswordFile ) )
					{
						final String pwdFileName = keystoreConfig.optString ( kSetting_KeystorePasswordFile, null );
						final File pwdFile = new File ( pwdFileName );
						try ( FileInputStream fis = new FileInputStream ( pwdFile ) )
						{
							final byte[] pwdData = StreamTools.readBytes ( fis );
							keystorePassword = new String ( pwdData ).trim ();
						}
						catch ( IOException x )
						{
							log.warn ( "There was a problem trying to read {}: {}", pwdFileName, x.getMessage () );
						}
					}

					final String keystoreType = keystoreConfig.optString ( kSetting_KeystoreType, kDefault_KeystoreType );

					// the keystore alias can be delivered directly in configuration, or determined by a scan of the keystore
					String keystoreAlias = kDefault_KeystoreAlias;
					if ( keystoreConfig.has ( kSetting_KeystoreAlias ) )
					{
						keystoreAlias = keystoreConfig.getString ( kSetting_KeystoreAlias );
					}
					else if ( keystoreConfig.optBoolean ( kSetting_KeystoreAliasScan, false ) )
					{
						keystoreAlias = scanKeystoreForPrivateKey ( keystoreFilename, keystorePassword, keystoreType );
					}

					connector.setScheme ( "https" );
					connector.setSecure ( true );
					connector.setProperty ( "keystoreFile", keystoreFilename );
					connector.setProperty ( "keystorePass", keystorePassword );
					connector.setProperty ( "keystoreType", keystoreType );
					connector.setProperty ( "keyAlias", keystoreAlias );
					connector.setProperty ( "clientAuth", "false" );
					connector.setProperty ( "sslProtocol", "TLS" );
					connector.setProperty ( "SSLEnabled", "true" );
					connector.setPort ( port );

					transferConnectorAttributes ( connector, httpsConfig.optJSONObject ( "tomcat" ) );

					fTomcat.getService ().addConnector ( connector );
		
					log.info ( "Service [{}] listens for HTTPS on {}.", fName, port );
				}
				else
				{
					log.info ( "Service [{}] will not listen for HTTPS.", fName );
				}
			}
		}
		catch ( JSONException x )
		{
			throw new BuildFailure ( x );
		}
	}

	@Override
	public synchronized void start () throws FailedToStart
	{
		try
		{
			final CHttpServlet hs = new CHttpServlet (
				fSettings,
				fLifeCycle,
				fMetrics,
				fInspector,
				fAccounts
			);
			for ( CHttpRouteInstaller router : getRouteInstallers () )
			{
				hs.addRouter ( router );
			}
			for ( CHttpFilter filter : getFilters () )
			{
				hs.addFilter ( filter );
			}

			final String servletName = "httpService";
			final Context rootCtx = fTomcat.addContext ( "", fWorkDir.getAbsolutePath () );
			Tomcat.addServlet ( rootCtx, servletName, hs );
			rootCtx.addServletMappingDecoded ( "/*", servletName );

			try
			{
				fTomcat.start ();
				fRunning = true;
			}
			catch ( LifecycleException e )
			{
				log.warn ( "Couldn't start tomcat." , e );
				throw new FailedToStart ( e );
			}
	
			log.info ( "Service [{}] is listening.", fName );
		}
		catch ( BuildFailure x )
		{
			throw new FailedToStart ( x );
		}
	}

	@Override
	public synchronized void requestFinish ()
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

	private final String fName;

	private final Tomcat fTomcat;
	private boolean fRunning;
	private final File fWorkDir;
	private final SessionLifeCycle fLifeCycle;

	@Deprecated
	private final JSONObject fSettings;
	
	private final IamService<?,?> fAccounts;
	private final MetricsCatalog fMetrics;
	private final CHttpObserverMgr fInspector;

	private static final Logger log = LoggerFactory.getLogger ( TomcatHttpService.class );

	private void transferConnectorAttributes ( Connector connector, JSONObject config )
	{
		if ( config == null ) return;

		JsonVisitor.forEachElement ( config, new ObjectVisitor<Object,JSONException> ()
		{
			@Override
			public boolean visit ( String key, Object val ) throws JSONException
			{
				connector.setProperty ( key, String.valueOf ( val ) );
				return true;
			}
		} );
	}

	private static String scanKeystoreForPrivateKey ( String keystoreFilename, String keystorePassword, String keystoreType )
	{
		try
		{
			log.info ( "Scanning {} for its first private key...", keystoreFilename );

			final KeyStore ks = KeyStore.getInstance ( keystoreType );
			ks.load ( new FileInputStream ( keystoreFilename ), keystorePassword.toCharArray () );

			log.info ( "Keystore {} loaded...", keystoreFilename );

			final Enumeration<String> enumeration = ks.aliases ();
			while ( enumeration.hasMoreElements () )
			{
				final String alias = enumeration.nextElement ();
				if ( ks.entryInstanceOf ( alias, KeyStore.PrivateKeyEntry.class ) )
				{
					log.info ( "Found private key {}.", alias );
					return alias;
				}
			}
		}
		catch ( IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException x )
		{
			log.warn ( "Exception inspecting keystore {} for alias: {}", keystoreFilename, x.getMessage () );
		}
		return "";
	}
}
