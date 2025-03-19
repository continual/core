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
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.TreeSet;

public class TomcatHttpService extends CHttpService
{
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

			final String tomcatBaseDir = sc.getExprEval ().evaluateText ( settings.optString ( kSetting_TomcatBaseDir, kDefault_TomcatBaseDir )  );
			if ( tomcatBaseDir != null && !tomcatBaseDir.isEmpty () )
			{
				fTomcat.setBaseDir ( tomcatBaseDir );
			}

			// FIXME: should default work dir be within base dir?
			final String servletWorkDir = settings.optString ( kSetting_ServletWorkDir, kDefault_ServletWorkDir );
			fWorkDir = new File ( servletWorkDir );
			if ( !fWorkDir.exists () )
			{
				if ( !fWorkDir.mkdirs () )
				{
					throw new BuildFailure ( "Couldn't create working directory " + fWorkDir.getAbsolutePath () );
				}
			}

			// set up the endpoints
			setupEndpoints ( fTomcat, settings );
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

	private void setupConnectorEndpoints ( final Tomcat tomcat, final JSONArray connectors ) throws BuildFailure
	{
		final TreeSet<Integer> portsInUse = new TreeSet<> ();

		for ( int i=0; i<connectors.length (); i++ )
		{
			final JSONObject connectorConfig = connectors.optJSONObject ( i );

			final String configName = connectorConfig.optString ( "name", "connector-" + i );

			// is this connector entry enabled?
			if ( !connectorConfig.optBoolean ("enabled", true ) )
			{
				log.info ( "Skipping disabled connector {}.", configName );
				continue;
			}

			// determine the port
			final boolean isHttps = connectorConfig.optBoolean("secure", false);
			final int port = connectorConfig.optInt ( kSetting_Port, isHttps ? kDefault_HttpsPort : kDefault_HttpPort );
			if ( port <= 0 )
			{
				throw new BuildFailure ("Connector " + configName + " cannot listen on port " + port + ". (To disable this entry, set enable=false in this connector config.)");
			}

			// is this port already setup?
			if ( portsInUse.contains ( port ) )
			{
				throw new BuildFailure ("Port " + port + " is already in use. Choose a different port for connector " + configName + ".");
			}

			// set up a connector and provide its port
			final Connector connector = new Connector ( Http11NioProtocol.class.getName() );
			connector.setPort ( port );
			portsInUse.add ( port );

			// if https, we have more config to do
			if ( isHttps )
			{
				final JSONObject keystoreConfig = connectorConfig.optJSONObject ( kSetting_Keystore );
				if ( keystoreConfig == null )
				{
					throw new BuildFailure ( "Connector " + configName + " is set to https but is missing the required " + kSetting_Keystore + " configuration block." );
				}

				try
				{
					String keystoreFilename = keystoreConfig.getString ( kSetting_KeystoreFile );
					final File keystoreFile = new File ( keystoreFilename );
					if ( !keystoreFile.isAbsolute () )
					{
						// tomcat requires an absolute filename, or it'll use env var CATALINA_HOME. We want a file
						// relative to the current working directory.
						final Path path = FileSystems.getDefault ().getPath ( "." ).toAbsolutePath ();
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
						} catch ( IOException x )
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
				}
				catch ( JSONException x )
				{
					throw new BuildFailure ( x );
				}
			}

			// get any overrides from the optional "tomcat" block
			transferConnectorAttributes ( connector, connectorConfig.optJSONObject ( "tomcat" ) );

			// add the connector to our tomcat instance
			tomcat.getService ().addConnector ( connector );
			log.info ( "Service [{}] terminates {} on port {}.", fName, ( isHttps ? "https" : "http" ), port );
		}
	}

	private void setupEndpoints ( Tomcat tomcat, JSONObject settings ) throws BuildFailure
	{
		// very old service set up may just use "port" and open http on that port.
		// newer services set up might have "http" and/or "https" setup blocks.
		// most recently we want to read an array of connector setups.

		// if the connectors array is present, use that. If not, convert what is present into
		// connector entries.
		JSONArray connectors = settings.optJSONArray ( "connectors" );
		if ( connectors == null )
		{
			connectors = new JSONArray ();

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

			if ( httpConfig != null )
			{
				connectors.put ( httpConfig );
			}
			if ( httpsConfig != null )
			{
				connectors.put ( httpsConfig );
			}
		}
		setupConnectorEndpoints ( tomcat, connectors );
	}

	private void transferConnectorAttributes ( final Connector connector, final JSONObject config )
	{
		if ( config == null ) return;

		JsonVisitor.forEachElement ( config, (ObjectVisitor<Object, JSONException>) ( key, val ) ->
		{
			connector.setProperty ( key, String.valueOf ( val ) );
			return true;
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

	private static final String kSetting_ServletWorkDir = "workDir";
	private static final String kDefault_ServletWorkDir = new File ( System.getProperty ( "java.io.tmpdir" )).getAbsolutePath ();

	private static final String kSetting_TomcatBaseDir = "baseDir";
	private static final String kDefault_TomcatBaseDir = "${CONTINUAL_TOMCAT_BASEDIR}";

	private static final String kSetting_Keystore = "keystore";

	private static final String kSetting_KeystoreFile = "file";

	private static final String kSetting_KeystoreType = "type";
	private static final String kDefault_KeystoreType = "JKS";

	private static final String kSetting_KeystoreAlias = "alias";
	private static final String kDefault_KeystoreAlias = "tomcat";

	private static final String kSetting_KeystoreAliasScan = "scanForAlias";

	private static final String kSetting_KeystorePassword = "password";
	private static final String kDefault_KeystorePassword = "changeme";

	private static final String kSetting_KeystorePasswordFile = "passwordFile";

	private static final String kSetting_Port = "port";
	private static final int kDefault_HttpPort = 8080;
	private static final int kDefault_HttpsPort = 8443;
}
