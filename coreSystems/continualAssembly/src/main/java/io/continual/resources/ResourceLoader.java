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

package io.continual.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import io.continual.resources.sources.AwsS3UriLoader;
import io.continual.resources.sources.ClassResourceLoader;
import io.continual.resources.sources.FileLoader;
import io.continual.resources.sources.HttpLoader;
import io.continual.resources.sources.JvmClassLoaderResourceLoader;
import io.continual.resources.sources.JvmSystemResourceLoader;
import io.continual.resources.sources.JvmThreadContextClassLoader;
import io.continual.util.data.TypeConvertor;

/**
 * ResourceLoader is a general purpose tool for loading a data stream by name.
 */
public class ResourceLoader
{
	/**
	 * Attempt to load the named resource using our standard sources.
	 * @param resource
	 * @return the resource input stream or null
	 * @throws IOException
	 */
	public static InputStream load ( String resource ) throws IOException
	{
		return new ResourceLoader().named ( resource ).load ();
	}

	/**
	 * Install standard sources, which are, in order:<br>
	 * <br>
	 * S3 URI (when available in the CLASSPATH)<br>
	 * HTTP URL<br>
	 * Filesystem name<br>
	 * JVM classloader with this class as the base path<br>
	 * JVM system classloader<br>
	 * <br>
	 * 
	 * @return this
	 */
	public ResourceLoader usingStandardSources ()
	{
		return usingStandardSources ( true );
	}

	/**
	 * Install standard sources, with control over using networked resources.<br>
	 * @param withNetwork
	 * 
	 * @return this
	 */
	public ResourceLoader usingStandardSources ( boolean withNetwork )
	{
		return usingStandardSources ( withNetwork, null );
	}

	/**
	 * Install standard sources, with control over using networked resources.<br>
	 * @param withNetwork
	 * @param referenceClass
	 * 
	 * @return this
	 */
	public ResourceLoader usingStandardSources ( boolean withNetwork, Class<?> referenceClass )
	{
		if ( withNetwork )
		{
			// we'll use S3 if the libraries are available
			if ( s3Available () )
			{
				usingSource ( new AwsS3UriLoader () );
			}
			usingSource ( new HttpLoader () );
		}

		// files...
		usingSource ( new FileLoader () );

		// possibly based on a given class or its classloader
		if ( referenceClass != null )
		{
			usingSource ( new ClassResourceLoader ( referenceClass ) );
			usingSource ( new JvmClassLoaderResourceLoader ( referenceClass ) );
		}

		// system resources...
		usingSource ( new JvmSystemResourceLoader () );
		usingSource ( new JvmThreadContextClassLoader () );

		return this;
	}

	/**
	 * Add a resource source. You can call this repeatedly to add a number of sources
	 * and they'll be searched in the order provided.
	 * @param rs
	 * @return this
	 */
	public ResourceLoader usingSource ( ResourceSource rs )
	{
		fSrcSpecd = true;
		fSources.add ( rs );
		return this;
	}

	/**
	 * Establish the name of the resource being sought
	 * @param name
	 * @return this
	 */
	public ResourceLoader named ( String name )
	{
		fNamed = name;
		return this;
	}

	/**
	 * Load the resource. Caller must close the input stream. If no sources were explicitly
	 * added to this resource loader, the standard set is used (see usingStandardSources).
	 * This call searches each source, in the order provided, for the named item, returning
	 * the first found, if any. If not found in any source, null is returned.
	 * @return an input stream if the resource is found
	 * @throws IOException
	 */
	public InputStream load () throws IOException 
	{
		if ( fNamed == null ) return null;

		// if the user didn't say, assume standard sources
		if ( !fSrcSpecd ) usingStandardSources ();

		for ( ResourceSource src : fSources )
		{
			if ( src.qualifies ( fNamed ) )
			{
				final InputStream result = src.loadResource ( fNamed );
				if ( result != null )
				{
					return result;
				}
			}
		}

		return null;
	}

	@Override
	public String toString ()
	{
		return fNamed;
	}
	
	private LinkedList<ResourceSource> fSources = new LinkedList<ResourceSource> ();
	private String fNamed = null;
	private boolean fSrcSpecd = false;

	private static boolean sCheckedS3 = false;
	private static boolean sS3Available = false;
	private static boolean sS3Disabled = !TypeConvertor.convertToBooleanBroad ( System.getenv ( "DRIFT_ALLOW_S3" ) );

	/**
	 * Check (first time only) if the Amazon S3 interface is available.
	 * @return true or false
	 */
	public static boolean s3Available ()
	{
		if ( !sCheckedS3 && !sS3Disabled )
		{
			sCheckedS3 = true;
			try
			{
				final Class<?> clazz = com.amazonaws.services.s3.AmazonS3.class;
				sS3Available = clazz != null;
			}
			catch ( NoClassDefFoundError x )
			{
				sS3Available = false;
			}
		}
		return sS3Available;
	}
}

