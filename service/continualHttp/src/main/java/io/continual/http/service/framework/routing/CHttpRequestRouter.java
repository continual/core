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

package io.continual.http.service.framework.routing;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.CHttpErrorHandler;
import io.continual.http.service.framework.context.CHttpRequest;
import io.continual.http.service.framework.context.CHttpRequestContext;

/**
 * A request router is configured with route sources and error handlers, then
 * used to route an incoming request to a request handler.
 */
public class CHttpRequestRouter
{
	/**
	 * no matching route exception
	 */
	public static class noMatchingRoute extends Exception
	{
		public noMatchingRoute ( String route ) { super ( "No route for '" + route + "'" ); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * construct a request router
	 */
	public CHttpRequestRouter () 
	{
		fSources = new LinkedList<> ();
		fErrorHandlers = new HashMap<>();
	}

	/**
	 * add a route source
	 * @param src
	 */
	public synchronized void addRouteSource ( CHttpRouteSource src )
	{
		addRouteSource ( src, false );
	}
	
	/**
	 * add a route source, specifying whether the source overwrites existing sources (withPriority)
	 * or is examined after existing sources.
	 * @param src
	 * @param withPriority
	 */
	public synchronized void addRouteSource ( CHttpRouteSource src, boolean withPriority )
	{
		if ( withPriority )
		{
			fSources.addFirst ( src );
		}
		else
		{
			fSources.addLast ( src );
		}
	}

	/**
	 * Provide a URL for redirects when there's no specific error handler for the exception.
	 * @param url
	 */
	public synchronized void setGeneralErrorRedirectUrl ( final String url )
	{
		fErrorHandlers.put ( Throwable.class, new CHttpErrorHandler ()
		{
			@Override
			public void handle ( CHttpRequestContext ctx, Throwable cause )
			{
				log.info ( "General error handler invoked, redirect to " + url );
				ctx.response ().redirect ( url );
			}
		} );
	}

	/**
	 * Set an error handler for a specific class of throwable. 
	 * @param x
	 * @param eh
	 */
	public synchronized void setHandlerForException ( Class<? extends Throwable> x, CHttpErrorHandler eh )
	{
		fErrorHandlers.put ( x, eh );
	}

	/**
	 * Given an incoming request, check each route source (in order) for a match. If the route source
	 * has a match, it's used to handle the request.
	 * 
	 * @param req
	 * @return a matching handler
	 * @throws noMatchingRoute
	 */
	public synchronized CHttpRouteInvocation route ( CHttpRequest req ) throws noMatchingRoute
	{
		final String verbIn = req.getMethod ();
		final String verb = verbIn.equalsIgnoreCase("HEAD")?"GET":verbIn;	// HEAD is GET without an entity response

		final String path = req.getPathInContext ();

		CHttpRouteInvocation route = null;
		for ( CHttpRouteSource src : fSources )
		{
			route = src.getRouteFor ( verb, path );
			if ( route != null )
			{
				break;
			}
		}

		if ( route == null )
		{
			log.info ( "No match for " + verb + " " + path );
			throw new noMatchingRoute ( path );
		}

		return route;
	}

	/**
	 * Find the proper handler for a throwable.
	 * @param cause
	 * @return an error handler, or null if none are applicable
	 */
	public synchronized CHttpErrorHandler route ( Throwable cause )
	{
		CHttpErrorHandler h = null;
		Class<?> c = cause.getClass ();
		while ( h == null && c != null )
		{
			h = fErrorHandlers.get ( c );
			if ( h == null )
			{
				c = c.getSuperclass ();
			}
		}
		return h;
	}

	/**
	 * Given a handler class and the name of one of its static methods, return a
	 * registered route to it. This version will not include session-specific routes.
	 * 
	 * @param c
	 * @param staticMethodName
	 * @return
	 */
	public synchronized String reverseRoute ( Class<?> c, String staticMethodName )
	{
		return reverseRoute ( c, staticMethodName, new HashMap<String,Object> () );
	}

	/**
	 * Given a handler class, the name of one of its static methods, and some arguments,
	 * return a registered route to the handler method.
	 * 
	 * @param c
	 * @param staticMethodName
	 * @param args
	 * @return
	 */
	public synchronized String reverseRoute ( Class<?> c, String staticMethodName, Map<String,Object> args )
	{
		String route = null;
		for ( CHttpRouteSource src : fSources )
		{
			route = src.getRouteTo ( c, staticMethodName, args );
			if ( route != null )
			{
				break;
			}
		}
		return route;
	}

	private final LinkedList<CHttpRouteSource> fSources;
	private final HashMap<Class<?>,CHttpErrorHandler> fErrorHandlers;

	private static final org.slf4j.Logger log = LoggerFactory.getLogger ( CHttpRequestRouter.class );
}
