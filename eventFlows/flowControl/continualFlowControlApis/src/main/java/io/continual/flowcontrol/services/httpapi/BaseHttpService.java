package io.continual.flowcontrol.services.httpapi;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.FlowControlService;
import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.flowcontrol.model.FlowControlCallContextBuilder;
import io.continual.flowcontrol.model.FlowControlJobDb;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService;
import io.continual.http.app.servers.routeInstallers.TypicalApiServiceRouteInstaller;
import io.continual.http.service.framework.CHttpService;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

public class BaseHttpService extends SimpleService implements FlowControlService
{
	public BaseHttpService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fJobDb = sc.getReqd ( config.optString ( "jobDb", "jobDb" ), FlowControlJobDb.class );
		fDeployApi = sc.getReqd ( config.optString ( "deployApi", "deployApi" ), FlowControlDeploymentService.class );

		final CHttpService http = sc.getReqd ( config.getString ( "httpService" ), CHttpService.class );

		http.addRouteInstaller ( new TypicalApiServiceRouteInstaller ()
			.registerRoutes ( "flowControlRoutes.conf", getClass(), new FlowControlRoutes<Identity> ( sc, config, this ) )
			.registerRoutes ( "configFetch.conf", getClass(), new ConfigFetch<Identity> ( sc, config, fDeployApi ) )
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

	private final FlowControlJobDb fJobDb;
	private final FlowControlDeploymentService fDeployApi;

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
}
