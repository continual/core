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


package io.continual.http.service.framework.routing.staticPaths;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.CHttpConnection;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.routing.CHttpRouteInvocation;
import io.continual.http.service.framework.routing.CHttpRouteSource;
import io.continual.http.service.framework.routing.playish.StaticDirHandler;
import io.continual.http.service.framework.routing.playish.StaticFileHandler;
import io.continual.http.util.http.standards.HttpMethods;
import io.continual.util.data.StreamTools;

/**
 * A static entry point routing source is a collection of routing entries for
 * mapping request paths to static files and directories.
 */
public class CHttpStaticPathRouter implements CHttpRouteSource
{
	public static String kMaxAge = StaticFileHandler.kMaxAge;

	public CHttpStaticPathRouter ( File baseDir ) throws IOException
	{
		fBaseDir = baseDir.getCanonicalFile ();
		if ( !fBaseDir.exists () || !fBaseDir.isDirectory () )
		{
			throw new IllegalArgumentException ( baseDir + " is not a directory." );
		}
	}

	/**
	 * This router will attempt to serve any path, assuming it's under the base
	 * directory. It handles GET/HEAD only, and rejects paths that are outside the base directory.
	 */
	@Override
	public synchronized CHttpRouteInvocation getRouteFor ( String verb, final String path, CHttpConnection forSession )
	{
		// only support GET (and HEAD)
		if ( !verb.equalsIgnoreCase ( HttpMethods.GET ) && !verb.equalsIgnoreCase ( HttpMethods.HEAD ) )
		{
			return null;
		}

		final File toServe = new File ( fBaseDir, path );
		return new CHttpRouteInvocation ()
		{
			@Override
			public void run ( CHttpRequestContext context )
				throws IOException,
					IllegalArgumentException,
					IllegalAccessException,
					InvocationTargetException
			{
				File in = toServe;
				if ( in.isDirectory () )
				{
					in = new File ( in, "index.html" );
				}

				final File canonical = in.getCanonicalFile ();
				if ( !canonical.getAbsolutePath ().startsWith ( fBaseDir.getAbsolutePath () ))
				{
					log.debug ( "ignoring [" + path + "] because it is outside of the base directory." );
					log.warn ( "404 [" + path + "]==>[" + path + "] (" + in.getAbsolutePath () + ")" );
					context.response ().sendError ( 404, path + " was not found on this server." );
					return;
				}
				
				// expiry. currently global.
				final int cacheMaxAge = context.systemSettings ().getInt ( kMaxAge, -1 );
				if ( cacheMaxAge > 0 )
				{
					context.response ().writeHeader ( "Cache-Control", "max-age=" + cacheMaxAge, true );
				}

				final String contentType = StaticDirHandler.mapToContentType ( in.getName () );

				try
				{
					final FileInputStream is = new FileInputStream ( in );
					final OutputStream os = context.response ().getStreamForBinaryResponse ( contentType );
					StreamTools.copyStream ( is, os );
				}
				catch ( FileNotFoundException e )
				{
					log.warn ( "404 [" + path + "]==>[" + path + "] (" + in.getAbsolutePath () + ")" );
					context.response ().sendError ( 404, path + " was not found on this server." );
				}
				catch ( IOException e )
				{
					log.warn ( "500 [" + toServe.getAbsolutePath () + "]: " + e.getMessage () );
					context.response ().sendError ( 500, e.getMessage () );
				}
			}
		};
	}

	/**
	 * Reverse routing to entry points doesn't apply here. Always returns null.
	 */
	@Override
	public String getRouteTo ( Class<?> c, String staticMethodName, Map<String, Object> args, CHttpConnection forSession )
	{
		return null;
	}

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( CHttpStaticPathRouter.class );

	private final File fBaseDir;
}
