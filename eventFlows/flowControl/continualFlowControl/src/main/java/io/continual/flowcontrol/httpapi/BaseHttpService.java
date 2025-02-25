package io.continual.flowcontrol.httpapi;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.flowcontrol.model.FlowControlCallContextBuilder;
import io.continual.flowcontrol.model.FlowControlDeploymentService;
import io.continual.flowcontrol.model.FlowControlJobDb;
import io.continual.flowcontrol.model.FlowControlRuntimeSystem;
import io.continual.flowcontrol.model.FlowControlService;
import io.continual.http.app.servers.routeInstallers.TypicalApiServiceRouteInstaller;
import io.continual.http.service.framework.CHttpService;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.standards.HttpStatusCodes;

public class BaseHttpService extends SimpleService implements FlowControlService
{
	public BaseHttpService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		// FIXME: these dbs need account-level scoping
		
		fJobDb = sc.getReqd ( config.optString ( "jobDb", "jobDb" ), FlowControlJobDb.class );
		fDeployApi = sc.getReqd ( config.optString ( "deployApi", "deployApi" ), FlowControlDeploymentService.class );
		fRuntimeEnv = sc.getReqd ( config.optString ( "runtimeSystemApi", "runtimeSystemApi" ), FlowControlRuntimeSystem.class );

		final CHttpService http = sc.getReqd ( config.getString ( "httpService" ), CHttpService.class );

		http.addRouteInstaller ( new TypicalApiServiceRouteInstaller ()
			.registerRoutes ( "flowControlRoutes.conf", getClass(), new FlowControlRoutes<Identity> ( sc, config, this ) )
			.registerRoutes ( "configFetch.conf", getClass(), new ConfigFetch<Identity> ( sc, config, fDeployApi ) )
			.registerRoutes ( "options.conf", BaseHttpService.class, new Options ( sc, config ) )
			.registerRoutes ( "health.conf", BaseHttpService.class, new Health ( sc, config ) )
			.registerErrorHandler ( FlowControlDeploymentService.ServiceException.class, HttpStatusCodes.k503_serviceUnavailable, null, log )
			.registerErrorHandler ( FlowControlDeploymentService.RequestException.class, HttpStatusCodes.k400_badRequest, null, log )
		);
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

	@Override
	public FlowControlRuntimeSystem getRuntimeSystem ( FlowControlCallContext fccc )
	{
		return fRuntimeEnv;
	}

	private final FlowControlJobDb fJobDb;
	private final FlowControlDeploymentService fDeployApi;
	private final FlowControlRuntimeSystem fRuntimeEnv;

	private static class BaseFlowControlCallContext implements FlowControlCallContext
	{
		public static class Builder implements FlowControlCallContextBuilder
		{
			public Builder ( )
			{
			}
			
			@Override
			public FlowControlCallContextBuilder asUser ( Identity i )
			{
				fUser = i;
				return this;
			}

			@Override
			public FlowControlCallContext build ()
			{
				return new BaseFlowControlCallContext ( this );
			}
			
			private Identity fUser;
		}

		@Override
		public Identity getUser ()
		{
			return fUser;
		}

		private BaseFlowControlCallContext ( Builder b )
		{
			fUser = b.fUser;
		}

		private final Identity fUser;
	}

	private static final Logger log = LoggerFactory.getLogger ( BaseHttpService.class );
}
