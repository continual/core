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

package io.continual.restHttp;

import java.io.IOException;
import java.util.LinkedList;

import javax.servlet.ServletException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.CHttpMetricNamer;
import io.continual.http.service.framework.CHttpServlet;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.iam.IamService;
import io.continual.metrics.MetricsCatalog;
import io.continual.services.ServiceContainer;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.impl.nvJsonObject;

public class HttpServlet extends CHttpServlet
{
	public HttpServlet ( ServiceContainer sc, IamService<?,?> accounts, SessionLifeCycle lc, JSONObject settings, MetricsCatalog metrics ) throws BuildFailure
	{
		super ( new nvJsonObject ( settings ), null, lc, metrics );

		fAccounts = accounts;
		fContainer = sc;
		fRouters = new LinkedList<HttpRouter> ();

		final JSONObject namer = settings.optJSONObject ( "metricsNamer" );
		if ( namer != null )
		{
			fMetricNamer = Builder.withBaseClass ( CHttpMetricNamer.class )
				.withClassNameInData ()
				.usingData ( namer )
				.build ()
			;
		}
		else
		{
			fMetricNamer = null;
		}
	}

	public void addRouter ( HttpRouter value )
	{
		fRouters.add ( value );
	}

	public ServiceContainer getServices ()
	{
		return fContainer;
	}

	public static HttpServlet getServlet ( CHttpRequestContext rc )
	{
		final CHttpServlet ds = rc.getServlet ();
		if ( !( ds instanceof HttpServlet ))
		{
			throw new IllegalStateException ( "This servlet is not an HttpServlet" );
		}
		return ((HttpServlet)ds);
	}
	
	public static ServiceContainer getServices ( CHttpRequestContext rc )
	{
		final CHttpServlet ds = rc.getServlet ();
		if ( !( ds instanceof HttpServlet ))
		{
			throw new IllegalStateException ( "This servlet is not an HttpServlet" );
		}
		return ((HttpServlet)ds).getServices ();
	}

	@Override
	public HttpUserSession createSession () throws NvReadable.MissingReqdSettingException
	{
		return new HttpUserSession ( fAccounts );
	}

	@Override
	protected void servletSetup () throws NvReadable.MissingReqdSettingException, ServletException
	{
		try
		{
			final NvReadable p = super.getSettings ();

			// setup request routing
			final CHttpRequestRouter rr = super.getRequestRouter ();
			for ( HttpRouter router : fRouters )
			{
				router.setupRouter ( this, rr, p );
			}

			log.info ( "The server is ready." );
		}
		catch ( IOException | BuildFailure e )
		{
			throw new ServletException ( e );
		}
	}

	@Override
	protected CHttpMetricNamer getMetricNamer ()
	{
		return fMetricNamer == null ? super.getMetricNamer () : fMetricNamer;
	}

	private final ServiceContainer fContainer;
	private final IamService<?,?> fAccounts;
	private final LinkedList<HttpRouter> fRouters;
	private final CHttpMetricNamer fMetricNamer;

	private static final Logger log = LoggerFactory.getLogger ( HttpServlet.class );
	private static final long serialVersionUID = 1L;
}
