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

package io.continual.http.app.servers.routeInstallers;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Set;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.app.servers.CorsOptionsRouter;
import io.continual.http.service.framework.CHttpErrorHandler;
import io.continual.http.service.framework.CHttpRouteInstaller;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.http.service.framework.routing.CHttpRouteSource;
import io.continual.http.service.framework.routing.playish.CHttpPlayishInstanceCallRoutingSource;
import io.continual.http.service.framework.routing.playish.CHttpPlayishRoutingFileSource;
import io.continual.resources.ResourceLoader;
import io.continual.util.nv.NvReadable;
import io.continual.util.standards.MimeTypes;

public class BaseRouteInstaller implements CHttpRouteInstaller
{
	/**
	 * Setup a base route installer
	 */
	public BaseRouteInstaller ()
	{
		fRouteEntries = new LinkedList<> ();
		fErrorHandlerEntries = new LinkedList<> ();
	}

	/**
	 * Setup a base route installer
	 * @param withCors
	 * @deprecated Specify allowed origins
	 */
	@Deprecated
	public BaseRouteInstaller ( boolean withCors )
	{
		this ( false, null );
	}

	/**
	 * Setup a base route installer
	 * @param generateCorsHeaders if true, generate cors headers. If false, no cors headers
	 * are generated here and allowedOrigins is ignored.
	 * @param allowedOrigins if not null, the list of allowed origins. If a
	 * 	call doesn't match (or doesn't provide an origin), no cors headers are sent.
	 * 	If null, a wildcard allowed origins is sent but not credentials cors header.
	 * @deprecated if a cors (options) handler is required, call "setupCorsHandler ( requestRouter, originSet )". 
	 */
	@Deprecated
	public BaseRouteInstaller ( boolean generateCorsHeaders, Set<String> allowedOrigins )
	{
		this ();
		log.error ( "If a cors (options) handler is required, call \"setupCorsHandler ( requestRouter, originSet )\"." );
	}

	public BaseRouteInstaller registerRouteSource ( CHttpRouteSource routeSource )
	{
		fRouteEntries.add ( routeSource );
		return this;
	}

	@Override
	public void setupRouter ( CHttpRequestRouter rr, NvReadable prefs ) throws IOException 
	{
		setupExceptionHandlers ( rr );
		for ( CHttpRouteSource rs : fRouteEntries )
		{
			rr.addRouteSource ( rs );
		}
	}

	public BaseRouteInstaller registerRoutes ( String routeFile, Object handler ) throws BuildFailure
	{
		return registerRoutes ( routeFile, this.getClass (), handler );
	}

	public BaseRouteInstaller registerRoutes ( String routeFile, Class<?> clazz, Object handler ) throws BuildFailure
	{
		try (
			final InputStream is = new ResourceLoader ()
				.usingStandardSources ( false, clazz )
				.named ( routeFile )
				.load ()
			;
		)
		{
			if ( is == null )
			{
				throw new BuildFailure ( "Unable to load " + routeFile + "." );
			}
			registerRoutes ( is, handler );
		}
		catch ( IOException x )
		{
			throw new BuildFailure ( x );
		}

		return this;
	}

	public BaseRouteInstaller registerRoutes ( InputStream routeFile, Object handler ) throws BuildFailure
	{
		if ( routeFile == null )
		{
			throw new BuildFailure ( "Received a null input stream for routes." );
		}

		try
		{
			registerRouteSource ( new CHttpPlayishInstanceCallRoutingSource<Object> ( handler, routeFile ) );
		}
		catch ( IOException x )
		{
			throw new BuildFailure ( x );
		}

		return this;
	}

	public BaseRouteInstaller registerStaticRoutes ( String routeFile, Class<?> clazz ) throws BuildFailure
	{
		try
		{
			registerRouteSource ( new CHttpPlayishRoutingFileSource ( new ResourceLoader ()
				.usingStandardSources ( false, clazz )
				.named ( routeFile ) )
			);
		}
		catch ( IOException x )
		{
			throw new BuildFailure ( x );
		}
		return this;
	}

	public BaseRouteInstaller registerErrorHandler ( Class<? extends Throwable> exClass, CHttpErrorHandler handler )
	{
		fErrorHandlerEntries.add ( new ErrHandlerEntry ( exClass, handler ) );
		return this;
	}

	public BaseRouteInstaller registerErrorHandler ( Class<? extends Throwable> exClass, int statusCode )
	{
		return registerErrorHandler ( exClass, statusCode, null, null );
	}

	public BaseRouteInstaller registerErrorHandler ( Class<? extends Throwable> exClass, int statusCode, Logger log )
	{
		return registerErrorHandler ( exClass, statusCode, null, log );
	}

	public BaseRouteInstaller registerErrorHandler ( Class<? extends Throwable> exClass, final int statusCode, final String configuredMsg )
	{
		return registerErrorHandler ( exClass, statusCode, configuredMsg, null );
	}

	public BaseRouteInstaller registerErrorHandler ( Class<? extends Throwable> exClass, final int statusCode, final String configuredMsg, Logger log )
	{
		return registerErrorHandler ( exClass, new CHttpErrorHandler ()
		{
			@Override
			public void handle ( CHttpRequestContext ctx, Throwable cause )
			{
				final String actualMsg = configuredMsg == null ? cause.getMessage () : configuredMsg;

				ctx.response ().sendStatusAndBody ( statusCode, 
					new JSONObject ()
						.put ( "error", statusCode )
						.put ( "message", actualMsg )
						.toString (),
					MimeTypes.kAppJson );

				if ( log != null )
				{
					log.warn ( actualMsg, cause );
				}
			}
		} );
	}

	/**
	 * Setup generic CORS headers for the router, allowing specific origins. This is equivalent
	 * to calling "addRouteSource ( new CorsOptionsRouter ( allowedOrigins ) )"
	 * @param rr a request router
	 * @param allowedOrigins
	 */
	protected void setupCorsHandler ( CHttpRequestRouter rr, Set<String> allowedOrigins )
	{
		rr.addRouteSource ( new CorsOptionsRouter ( allowedOrigins ) );
	}

	protected void setupExceptionHandlers ( CHttpRequestRouter rr )
	{
		for ( ErrHandlerEntry ehe : fErrorHandlerEntries )
		{
			rr.setHandlerForException ( ehe.getExceptionClass (), ehe.getHandler () );
		}
	}

	private static class ErrHandlerEntry
	{
		public ErrHandlerEntry ( Class<? extends Throwable> exClass, CHttpErrorHandler handler )
		{
			fEx = exClass;
			fHandler = handler;
		}

		public Class<? extends Throwable> getExceptionClass () { return fEx; }
		public CHttpErrorHandler getHandler () { return fHandler; }

		private final Class<? extends Throwable> fEx;
		private final CHttpErrorHandler fHandler;
	}
	
	private final LinkedList<CHttpRouteSource> fRouteEntries;
	private final LinkedList<ErrHandlerEntry> fErrorHandlerEntries;

	private static final Logger log = LoggerFactory.getLogger ( BaseRouteInstaller.class );
}
