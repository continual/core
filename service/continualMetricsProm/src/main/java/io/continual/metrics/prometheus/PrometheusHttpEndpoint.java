package io.continual.metrics.prometheus;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.CHttpConnection;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.context.CHttpResponse;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.http.service.framework.routing.CHttpRouteInvocation;
import io.continual.http.service.framework.routing.CHttpRouteSource;
import io.continual.restHttp.BaseApiServiceRouter;
import io.continual.restHttp.HttpService;
import io.continual.restHttp.HttpServlet;
import io.continual.services.ServiceContainer;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;
import io.continual.util.nv.NvReadable;
import io.continual.util.standards.HttpStatusCodes;

public class PrometheusHttpEndpoint extends HttpService
{
	public PrometheusHttpEndpoint ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config );

		final HttpService http = sc.get ( config.getString ( "httpService" ), HttpService.class );
		if ( http == null ) throw new BuildFailure ( "An HTTP service (\"httpService\") is required in the HttpApiService configuration." );

		http.addRouter ( "httpApi", new BaseApiServiceRouter ()
		{
			@Override
			public void setupRouter ( HttpServlet servlet, CHttpRequestRouter rr, NvReadable p ) throws IOException, BuildFailure
			{
				super.setupExceptionHandlers ( servlet, rr, p );

				rr.addRouteSource ( new MetricsRouter () );
			}
		} );
	}

	private static class MetricsRouter implements CHttpRouteSource
	{
		@Override
		public CHttpRouteInvocation getRouteFor ( String verb, String path, CHttpConnection forSession )
		{
			if ( verb != null && verb.equalsIgnoreCase ( "GET" ) && path != null && path.equals ( "metrics" ) )
			{
				return new CHttpRouteInvocation ()
				{
					@Override
					public void run ( CHttpRequestContext context )
						throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
					{
						final CHttpResponse reply = context.response ();
						reply.writeHeader ( "Access-Control-Allow-Origin", "*" );
						reply.writeHeader ( "Access-Control-Allow-Methods", "DELETE, GET, OPTIONS, PATCH, POST, PUT" );
						reply.writeHeader ( "Access-Control-Max-Age", "3600" );
						reply.setStatus ( HttpStatusCodes.k204_noContent );
					}

					@Override
					public Path getRouteNameForMetrics ()
					{
						// FIXME: this is just to complete the interface.
						return Path.getRootPath ().makeChildItem ( Name.fromString ( "metrics" ) );
					}
				};
			}
			return null;
		}

		@Override
		public String getRouteTo ( Class<?> c, String staticMethodName, Map<String, Object> args, CHttpConnection forSession )
		{
			return null;
		}
		
	}
}
