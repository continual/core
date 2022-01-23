package io.continual.flowcontrol.svcapi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.FlowControlApi;
import io.continual.flowcontrol.FlowControlCallContext;
import io.continual.flowcontrol.FlowControlCallContextBuilder;
import io.continual.flowcontrol.FlowControlService;
import io.continual.flowcontrol.controlapi.ConfigTransferService;
import io.continual.flowcontrol.controlapi.FlowControlDeploymentService;
import io.continual.flowcontrol.endpoints.ConfigFetch;
import io.continual.flowcontrol.endpoints.FlowControlRoutes;
import io.continual.flowcontrol.jobapi.FlowControlJobDb;
import io.continual.http.service.framework.CHttpConnection;
import io.continual.http.service.framework.CHttpErrorHandler;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.context.CHttpResponse;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.http.service.framework.routing.CHttpRouteInvocation;
import io.continual.http.service.framework.routing.CHttpRouteSource;
import io.continual.http.service.framework.routing.playish.CHttpPlayishInstanceCallRoutingSource;
import io.continual.iam.IamServiceManager;
import io.continual.iam.identity.Identity;
import io.continual.restHttp.BaseApiServiceRouter;
import io.continual.restHttp.HttpService;
import io.continual.restHttp.HttpServlet;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;
import io.continual.util.nv.NvReadable;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.standards.MimeTypes;

public class BaseHttpService extends SimpleService implements FlowControlService
{
	public BaseHttpService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fJobDb = sc.get ( config.optString ( "jobDb", "jobDb" ), FlowControlJobDb.class );
		if ( fJobDb == null ) throw new BuildFailure ( "No job database found." );

		fDeployApi = sc.get ( config.optString ( "deployApi", "deployApi" ), FlowControlDeploymentService.class );
		if ( fDeployApi == null ) throw new BuildFailure ( "No deploy service found." );

		fConfigTransfer = sc.get ( config.optString ( "configTransfer", "configTransfer" ), ConfigTransferService.class );
		if ( fConfigTransfer == null ) throw new BuildFailure ( "No config transfer service found." );

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
				addRoutes ( rr, "configFetch.conf", new ConfigFetch<Identity> ( fConfigTransfer ) );

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

	@Override
	public FlowControlCallContextBuilder createtContextBuilder ()
	{
		return new BaseFlowControlCallContext.Builder ();
	}

	@Override
	public FlowControlJobDb getJobDb ( FlowControlCallContext fccc )
	{
		return fJobDb;
	}

	@Override
	public FlowControlDeploymentService getDeployer ( FlowControlCallContext fccc )
	{
		return fDeployApi;
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

					@Override
					public Path getRouteNameForMetrics ()
					{
						// FIXME: this is just to complete the interface.
						return Path.getRootPath ().makeChildItem ( Name.fromString ( "options" ) );
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

	private final FlowControlJobDb fJobDb;
	private final FlowControlDeploymentService fDeployApi;
	private final ConfigTransferService fConfigTransfer;

	private static final Logger log = LoggerFactory.getLogger ( BaseHttpService.class );
	
	private static final String skAllowHeadersValue = 
		"Content-Type, " +
		"Authorization" 
//		ApiContextHelper.kDefault_AuthLineHeader + ", " +
//		ApiContextHelper.kDefault_DateLineHeader + ", " +
//		ApiContextHelper.kDefault_MagicLineHeader
	;
}
