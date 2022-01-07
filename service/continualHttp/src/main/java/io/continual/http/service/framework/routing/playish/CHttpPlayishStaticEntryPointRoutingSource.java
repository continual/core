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

package io.continual.http.service.framework.routing.playish;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.CHttpConnection;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.routing.CHttpRouteInvocation;
import io.continual.http.service.framework.routing.CHttpRouteSource;
import io.continual.util.naming.Path;

/**
 * A static entry point routing source is a collection of routing entries for mapping request
 * paths to static files and directories.
 */
public class CHttpPlayishStaticEntryPointRoutingSource implements CHttpRouteSource
{
	public CHttpPlayishStaticEntryPointRoutingSource ()
	{
		fPathList = new LinkedList<>();
		fPackages = new LinkedList<>();
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
	public synchronized CHttpPlayishStaticEntryPointRoutingSource addRoute ( String verb, String path, String action )
	{
		if ( action.startsWith ( kStaticDirTag ) )
		{
			final CHttpPathInfo pe = CHttpPathInfo.processPath ( verb, path + ".*" );
			pe.setHandler ( new StaticDirHandler ( path, action.substring ( kStaticDirTag.length () ) ) );
			fPathList.add ( pe );
		}
		else if ( action.startsWith ( kStaticFileTag ) )
		{
			final CHttpPathInfo pe = CHttpPathInfo.processPath ( verb, path );
			pe.setHandler ( new StaticFileHandler ( path, action.substring ( kStaticFileTag.length () ) ) );
			fPathList.add ( pe );
		}
		else if ( action.startsWith ( kRedirectTag ) )
		{
			final CHttpPathInfo pe = CHttpPathInfo.processPath ( verb, path );
			final String loc = action.substring ( kRedirectTag.length () );
			pe.setHandler ( new RedirectHandler ( loc ) );
			fPathList.add ( pe );
		}
		else
		{
			final CHttpPathInfo pe = CHttpPathInfo.processPath ( verb, path );
			pe.setHandler ( new StaticJavaEntryAction ( action, pe.getArgs(), fPackages ) );
			fPathList.add ( pe );
		}
		return this;
	}

	/**
	 * Get a route invocation for a given verb+path, or null.
	 */
	@Override
	public synchronized CHttpRouteInvocation getRouteFor ( String verb, String path, CHttpConnection forSession )
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
	public String getRouteTo ( Class<?> c, String staticMethodName, Map<String, Object> args, CHttpConnection forSession )
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

	private final LinkedList<String> fPackages;
	private final LinkedList<CHttpPathInfo> fPathList;

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( CHttpPlayishStaticEntryPointRoutingSource.class );

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
		log.debug ( "Clearing routes within this static route source." );
		fPathList.clear ();
	}

	protected synchronized void addPackage ( String pkg )
	{
		fPackages.add ( pkg );
	}

	private static final String kStaticDirTag = "staticDir:";
	private static final String kStaticFileTag = "staticFile:";
	private static final String kRedirectTag = "redirect:";
}
