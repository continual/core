/*
 *  Copyright (c) 2006-2025 Continual.io. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.continual.http.service.framework;

import io.continual.builder.Builder.BuildFailure;
import io.continual.util.data.StreamTools;
import io.continual.util.time.Clock;
import org.apache.catalina.connector.Connector;
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

public class TomcatTlsConfig
{
	public static class Builder
	{
		public Builder fromJsonConfig ( JSONObject keystoreConfig ) throws BuildFailure
		{
			try
			{
				// find our keystore file
				fKeystoreFile = makeAbsolute ( keystoreConfig.getString ( kSetting_KeystoreFile ) );

				// the keystore password can be delivered directly in configuration, or loaded from a file
				if ( keystoreConfig.has ( kSetting_KeystorePassword ) )
				{
					fLiteralPassword = keystoreConfig.getString ( kSetting_KeystorePassword );
				}
				else if ( keystoreConfig.has ( kSetting_KeystorePasswordFile ) )
				{
					fPasswordFile = keystoreConfig.getString ( kSetting_KeystorePasswordFile );
				}

				// what is the type of our keystore?
				fKeystoreType = keystoreConfig.optString ( kSetting_KeystoreType, kDefault_KeystoreType );

				// the keystore alias can be delivered directly in configuration, or determined by a scan of the keystore
				fKeystoreAlias = keystoreConfig.optString ( kSetting_KeystoreAlias, fKeystoreAlias );
				fScanForAlias = keystoreConfig.optBoolean ( kSetting_KeystoreAliasScan, fScanForAlias );
			}
			catch ( JSONException x )
			{
				throw new BuildFailure ( x );
			}
			return this;
		}

		public TomcatTlsConfig build ()
		{
			return new TomcatTlsConfig ( this );
		}

		private String fKeystoreFile = null;
		private String fLiteralPassword = null;
		private String fPasswordFile = null;
		private String fKeystoreType = kDefault_KeystoreType;
		private String fKeystoreAlias = kDefault_KeystoreAlias;
		private boolean fScanForAlias = false;
	}

	/**
	 * Does this TLS config have a pending update?
	 * @return true if there's a pending update
	 */
	public boolean hasUpdate ()
	{
		// if either our keystore file or password file has changed (when using a password file), there's a pending update
		return
			( new File ( fKeystoreFilename ).lastModified () > fLastConnectorWriteMs ) ||
			( fKeystorePassword == null && new File ( fKeystorePasswordFile ).lastModified () > fLastConnectorWriteMs )
		;
	}

	/**
	 * Write this TLS configuration to the given connector.
	 * @param connector a Tomcat connector
	 */
	public void writeToConnector ( Connector connector )
	{
		connector.setScheme ( "https" );
		connector.setSecure ( true );
		connector.setProperty ( "keystoreFile", fKeystoreFilename );
		connector.setProperty ( "keystorePass", getKeystorePassword () );
		connector.setProperty ( "keystoreType", fKeystoreType );
		connector.setProperty ( "keyAlias", getKeystoreAlias () );
		connector.setProperty ( "clientAuth", "false" );
		connector.setProperty ( "sslProtocol", "TLS" );
		connector.setProperty ( "SSLEnabled", "true" );

		fLastConnectorWriteMs = Clock.now ();
	}

	private final String fKeystoreFilename;
	private final String fKeystorePassword;
	private final String fKeystorePasswordFile;
	private final String fKeystoreType;
	private final String fKeystoreAlias;
	private final boolean fScanForAlias;

	private long fLastConnectorWriteMs;
	private String fCachedPassword;
	private long fLastPasswordFileRead;

	private static final String kSetting_KeystoreFile = "file";

	private static final String kSetting_KeystoreType = "type";
	private static final String kDefault_KeystoreType = "JKS";

	private static final String kSetting_KeystoreAlias = "alias";
	private static final String kDefault_KeystoreAlias = "tomcat";

	private static final String kSetting_KeystoreAliasScan = "scanForAlias";

	private static final String kSetting_KeystorePassword = "password";
	private static final String kDefault_KeystorePassword = "changeme";

	private static final String kSetting_KeystorePasswordFile = "passwordFile";

	private static final Logger log = LoggerFactory.getLogger ( TomcatTlsConfig.class );

	private TomcatTlsConfig ( Builder builder )
	{
		fKeystoreFilename = builder.fKeystoreFile;
		fKeystorePassword = builder.fLiteralPassword;
		fKeystorePasswordFile = builder.fPasswordFile;
		fKeystoreType = builder.fKeystoreType;
		fKeystoreAlias = builder.fKeystoreAlias;
		fScanForAlias = builder.fScanForAlias;

		fLastConnectorWriteMs = 0L;
		fCachedPassword = null;
		fLastPasswordFileRead = 0L;
	}

	private String getKeystorePassword ()
	{
		// if it's set directly, just use that
		if ( fKeystorePassword != null ) return fKeystorePassword;

		// We're loading the value from a file...
		final File pwdFile = new File ( fKeystorePasswordFile );

		// has the file changed since our last read?
		final long lastModified = pwdFile.lastModified ();

		// if we have a cached password, and the file hasn't changed, use the cached value
		if ( fCachedPassword != null && lastModified <= fLastPasswordFileRead )
		{
			return fCachedPassword;
		}

		// read the file
		try ( FileInputStream fis = new FileInputStream ( pwdFile ) )
		{
			fCachedPassword = new String ( StreamTools.readBytes ( fis ) ).trim ();
			fLastPasswordFileRead = lastModified;
			return fCachedPassword;
		}
		catch ( IOException x )
		{
			log.warn ( "There was a problem trying to read {}: {}", fKeystorePasswordFile, x.getMessage () );
		}

		return kDefault_KeystorePassword;
	}

	private String getKeystoreAlias ()
	{
		if ( fScanForAlias )
		{
			return scanKeystoreForPrivateKey ();
		}
		else
		{
			return fKeystoreAlias;
		}
	}

	// tomcat requires an absolute filename, or it'll use env var CATALINA_HOME. We want a file
	// relative to the current working directory.
	private static String makeAbsolute ( String filename )
	{
		if ( filename == null ) return null;

		String result = filename;

		final File keystoreFile = new File ( filename );
		if ( !keystoreFile.isAbsolute () )
		{
			final Path path = FileSystems.getDefault ().getPath ( "." ).toAbsolutePath ();
			final File file = new File ( path.toFile (), filename );
			result = file.getAbsolutePath ();
			log.info ( "Replacing path {} with absolute path {} .", filename, result );
		}

		return result;
	}

	private String scanKeystoreForPrivateKey (  )
	{
		try
		{
			log.info ( "Scanning {} for its first private key...", fKeystoreFilename );

			final KeyStore ks = KeyStore.getInstance ( fKeystoreType );
			ks.load ( new FileInputStream ( fKeystoreFilename ), getKeystorePassword ().toCharArray () );

			log.info ( "Keystore {} loaded...", fKeystoreFilename );

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
			log.warn ( "Exception inspecting keystore {} for alias: {}", fKeystoreFilename, x.getMessage () );
		}
		return "";
	}
}
