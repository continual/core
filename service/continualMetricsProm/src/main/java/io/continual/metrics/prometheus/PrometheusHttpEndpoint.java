package io.continual.metrics.prometheus;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.app.servers.routeInstallers.TypicalApiServiceRouteInstaller;
import io.continual.http.service.framework.CHttpService;
import io.continual.http.service.framework.TomcatHttpService;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.context.CHttpResponse;
import io.continual.http.service.framework.routing.CHttpRouteInvocation;
import io.continual.http.service.framework.routing.CHttpRouteSource;
import io.continual.services.ServiceContainer;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;
import io.continual.util.standards.HttpStatusCodes;

public class PrometheusHttpEndpoint extends TomcatHttpService
{
	public PrometheusHttpEndpoint ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config );

		final CHttpService http = sc.get ( config.getString ( "httpService" ), CHttpService.class );
		if ( http == null ) throw new BuildFailure ( "An HTTP service (\"httpService\") is required in the HttpApiService configuration." );

		http.addRouteInstaller ( new TypicalApiServiceRouteInstaller ()
			.registerRouteSource ( new MetricsRouter () )
		);
	}

	private static class MetricsRouter implements CHttpRouteSource
	{
		@Override
		public CHttpRouteInvocation getRouteFor ( String verb, String path )
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
		public String getRouteTo ( Class<?> c, String staticMethodName, Map<String, Object> args )
		{
			return null;
		}
	}
}
