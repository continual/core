package io.continual.iam.apiserver;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.CHttpConnection;
import io.continual.http.service.framework.CHttpErrorHandler;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.context.CHttpResponse;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.http.service.framework.routing.CHttpRouteInvocation;
import io.continual.http.service.framework.routing.CHttpRouteSource;
import io.continual.http.service.framework.routing.playish.CHttpPlayishInstanceCallRoutingSource;
import io.continual.http.service.framework.routing.playish.CHttpPlayishStaticEntryPointRoutingSource;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.http.util.http.standards.MimeTypes;
import io.continual.iam.IamServiceManager;
import io.continual.iam.apiserver.endpoints.AuthApiHandler;
import io.continual.iam.apiserver.endpoints.IamApiHandler;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.restHttp.BaseApiServiceRouter;
import io.continual.restHttp.HttpService;
import io.continual.restHttp.HttpServlet;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.nv.NvReadable;

public class IamHttpApiService extends SimpleService
{
	public IamHttpApiService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		final HttpService http = sc.get ( config.getString ( "httpService" ), HttpService.class );
		if ( http == null ) throw new BuildFailure ( "An HTTP service (\"httpService\") is required in the API service configuration." );

		final IamServiceManager<?,?> accts = sc.get ( config.getString ( "accounts" ), IamServiceManager.class );
		if ( accts == null ) throw new BuildFailure ( "An HTTP service (\"accounts\") is required in the configuration." );

		http.addRouter ( "httpApi", new BaseApiServiceRouter ()
		{
			@Override
			public void setupRouter ( HttpServlet servlet, CHttpRequestRouter rr, NvReadable p ) throws IOException
			{
				super.setupExceptionHandlers ( servlet, rr, p );

				// general purpose OPTIONS handler
				rr.addRouteSource ( new CorsOptionsRouter () );

				// add a path to the API guide resources
				final CHttpPlayishStaticEntryPointRoutingSource guideSrc = new CHttpPlayishStaticEntryPointRoutingSource ();
				guideSrc.addRoute ( "GET", "/guide", "staticDir:com/rathravane/labels/guide;index.html" );
				rr.addRouteSource ( guideSrc );

				// add routes
				addRoutes ( rr, "authRoutes.conf", new AuthApiHandler () );
				addRoutes ( rr, "iamRoutes.conf", new IamApiHandler ( accts, config.getString ( "accessRequired" ) ) );

				// catch IAM service outage
				rr.setHandlerForException ( IamSvcException.class,
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

	private static final Logger log = LoggerFactory.getLogger ( IamHttpApiService.class );

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

	private static final String skAllowHeadersValue = 
		"Content-Type, " +
		"Authorization" 
//		ApiContextHelper.kDefault_AuthLineHeader + ", " +
//		ApiContextHelper.kDefault_DateLineHeader + ", " +
//		ApiContextHelper.kDefault_MagicLineHeader
	;
}
