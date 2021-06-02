package io.continual.flowcontrol.impl.common;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.FlowControlApi;
import io.continual.flowcontrol.FlowControlService;
import io.continual.flowcontrol.endpoints.FlowControlRoutes;
import io.continual.http.service.framework.CHttpConnection;
import io.continual.http.service.framework.CHttpErrorHandler;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.context.CHttpResponse;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.http.service.framework.routing.CHttpRouteInvocation;
import io.continual.http.service.framework.routing.CHttpRouteSource;
import io.continual.http.service.framework.routing.playish.CHttpPlayishInstanceCallRoutingSource;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.http.util.http.standards.MimeTypes;
import io.continual.iam.IamServiceManager;
import io.continual.iam.identity.Identity;
import io.continual.restHttp.BaseApiServiceRouter;
import io.continual.restHttp.HttpService;
import io.continual.restHttp.HttpServlet;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.nv.NvReadable;

public abstract class BaseHttpService extends SimpleService implements FlowControlService
{
	public BaseHttpService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		final HttpService http = sc.get ( config.getString ( "httpService" ), HttpService.class );
		if ( http == null ) throw new BuildFailure ( "An HTTP service (\"httpService\") is required in the API service configuration." );

		@SuppressWarnings("unchecked")
		final IamServiceManager<Identity,?> accts = sc.get ( config.getString ( "accounts" ), IamServiceManager.class );
		if ( accts == null ) throw new BuildFailure ( "An HTTP service (\"accounts\") is required in the configuration." );

		final FlowControlService fcs = this;

		http.addRouter ( "httpApi", new BaseApiServiceRouter ()
		{
			@Override
			public void setupRouter ( HttpServlet servlet, CHttpRequestRouter rr, NvReadable p ) throws IOException
			{
				super.setupExceptionHandlers ( servlet, rr, p );

				// general purpose OPTIONS handler
				rr.addRouteSource ( new CorsOptionsRouter () );

				// add routes
				addRoutes ( rr, "flowControlRoutes.conf", new FlowControlRoutes<Identity> ( accts, fcs ) );

				// catch IAM service outage
				rr.setHandlerForException ( FlowControlApi.FlowControlApiException.class,
					new CHttpErrorHandler ()
					{
						@Override
						public void handle ( CHttpRequestContext ctx, Throwable cause )
						{
							ctx.response ().sendErrorAndBody ( HttpStatusCodes.k503_serviceUnavailable, 
								new JSONObject ()
									.put ( "error", HttpStatusCodes.k503_serviceUnavailable )
									.put ( "message", cause.getMessage () )
									.toString (),
								MimeTypes.kAppJson );
						}
					} );
			}
		} );
	}

	private void addRoutes ( CHttpRequestRouter rr, String routeFile, Object handler ) throws IOException
	{
		log.debug ( "Loading routes from " + routeFile );
		final URL url = this.getClass ().getResource ( routeFile );
		rr.addRouteSource ( new CHttpPlayishInstanceCallRoutingSource<Object> ( handler, url ) );
	}

	private static class CorsOptionsRouter implements CHttpRouteSource
	{
		@Override
		public CHttpRouteInvocation getRouteFor ( String verb, String path, CHttpConnection forSession )
		{
			if ( verb != null && verb.equalsIgnoreCase ( "OPTIONS" ) )
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
						reply.writeHeader ( "Access-Control-Allow-Headers", skAllowHeadersValue );
						reply.setStatus ( HttpStatusCodes.k204_noContent );
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

	private static final Logger log = LoggerFactory.getLogger ( BaseHttpService.class );
	
	private static final String skAllowHeadersValue = 
		"Content-Type, " +
		"Authorization" 
//		ApiContextHelper.kDefault_AuthLineHeader + ", " +
//		ApiContextHelper.kDefault_DateLineHeader + ", " +
//		ApiContextHelper.kDefault_MagicLineHeader
	;
}
