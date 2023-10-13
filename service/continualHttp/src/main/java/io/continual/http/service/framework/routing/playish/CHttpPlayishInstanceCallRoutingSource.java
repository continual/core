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

package io.continual.http.service.framework.routing.playish;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.routing.CHttpRouteInvocation;
import io.continual.http.service.framework.routing.CHttpRouteSource;
import io.continual.util.naming.Path;

/**
 * This routing source routes to methods on an object instance.
 */
public class CHttpPlayishInstanceCallRoutingSource<T> implements CHttpRouteSource
{
	public CHttpPlayishInstanceCallRoutingSource ( T instance, URL url ) throws IOException
	{
		fInstance = instance;
		fPathList = new LinkedList<CHttpPathInfo> ();
		fPackages = new LinkedList<String> ();

		if ( url == null )
		{
			throw new IOException ( "URL for routing file is null in CHttpPlayishInstanceCallRoutingSource ( URL u )" );
		}
		loadRoutes ( url );
	}

	public CHttpPlayishInstanceCallRoutingSource ( T instance, InputStream is ) throws IOException
	{
		fInstance = instance;
		fPathList = new LinkedList<CHttpPathInfo> ();
		fPackages = new LinkedList<String> ();

		loadRoutes ( is );
	}

	/**
	 * Add a verb and path route with an action string. The action can start with "staticDir:" or
	 * "staticFile:". The remainder of the string is used as a relative filename to the dir (staticDir:), or 
	 * as a filename (staticFile:).
	 * @param verb
	 * @param path
	 * @param action
	 * @return this object (for use in chaining the add calls)
	 */
	public synchronized CHttpPlayishInstanceCallRoutingSource<T> addRoute ( String verb, String path, String action )
	{
		final CHttpPathInfo pe = CHttpPathInfo.processPath ( verb, path );
		pe.setHandler ( new InstanceEntryAction<T> ( fInstance, action, pe.getArgs(), fPackages ) );
		fPathList.add ( pe );

		return this;
	}

	/**
	 * Get a route invocation for a given verb+path, or null.
	 */
	@Override
	public synchronized CHttpRouteInvocation getRouteFor ( String verb, String path )
	{
		CHttpRouteInvocation selected = null;
		for ( CHttpPathInfo pe : fPathList )
		{
			final List<String> args = pe.matches ( verb, path );
			if ( args != null )
			{
				selected = getInvocation ( pe, args );
				break;
			}
		}
		return selected;
	}

	/**
	 * Get the URL that reaches a given static method with the given arguments. 
	 */
	@Override
	public String getRouteTo ( Class<?> c, String staticMethodName, Map<String, Object> args )
	{
		final String fullname = c.getName() + "." + staticMethodName;
		for ( CHttpPathInfo pe : fPathList )
		{
			if ( pe.invokes ( fullname ) )
			{
				return pe.makePath ( args );
			}
		}
		return null;
	}

	private final T fInstance;
	private final LinkedList<String> fPackages;
	private final LinkedList<CHttpPathInfo> fPathList;

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( CHttpPlayishInstanceCallRoutingSource.class );

	protected Invocation getInvocation ( CHttpPathInfo pe, List<String> args )
	{
		return new Invocation ( pe, args );
	}

	protected class Invocation implements CHttpRouteInvocation
	{
		public Invocation ( CHttpPathInfo pe, List<String> args )
		{
			fPe = pe;
			fArgs = args;
		}

		@Override
		public void run ( CHttpRequestContext ctx ) throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
		{
			fPe.getHandler ().handle ( ctx, fArgs );
		}

		@Override
		public Path getRouteNameForMetrics ()
		{
			String pathPart = fPe.getPath();
			if ( !pathPart.startsWith ( "/" ) )
			{
				pathPart = "/" + pathPart;
			}
			return Path.fromString ( "/" + fPe.getVerb () + pathPart );
		}

		private final CHttpPathInfo fPe;
		private final List<String> fArgs;
	}

	protected synchronized void clearRoutes ()
	{
		log.debug ( "Clearing routes within this instance route source." );
		fPathList.clear ();
	}

	protected synchronized void addPackage ( String pkg )
	{
		fPackages.add ( pkg );
	}


	private synchronized void loadRoutes ( URL u ) throws IOException
	{
		loadRoutes ( new InputStreamReader ( u.openStream () ) );
	}

	private synchronized void loadRoutes ( InputStream is ) throws IOException
	{
		loadRoutes ( new InputStreamReader ( is ) );
	}

	private synchronized void loadRoutes ( Reader r ) throws IOException
	{
		clearRoutes ();

		final BufferedReader fr = new BufferedReader ( r );
		
		String line;
		while ( ( line = fr.readLine () ) != null )
		{
			line = line.trim ();
			if ( line.length () > 0 && !line.startsWith ( "#" ) )
			{
				processLine ( line );
			}
		}
	}

	private void processLine ( String line )
	{
		try
		{
			final StringTokenizer st = new StringTokenizer ( line );
			final String verb = st.nextToken ();
			if ( verb.toLowerCase ().equals ( "package" ) )
			{
				final String pkg = st.nextToken ();
				addPackage ( pkg );
			}
			else
			{
				final String path = st.nextToken ();
				final String action = st.nextToken ();
				addRoute ( verb, path, action );
			}
		}
		catch ( NoSuchElementException e )
		{
			log.warn ( "There was an error processing route config line: \"" + line + "\"" );
		}
		catch ( IllegalArgumentException e )
		{
			log.warn ( "There was an error processing route config line: \"" + line + "\": " + e.getMessage () );
		}
	}
}
