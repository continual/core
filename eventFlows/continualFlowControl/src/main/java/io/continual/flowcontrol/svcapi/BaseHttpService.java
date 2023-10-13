package io.continual.flowcontrol.svcapi;

import org.json.JSONObject;

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
		fJobDb = sc.getReqd ( config.optString ( "jobDb", "jobDb" ), FlowControlJobDb.class );
		fDeployApi = sc.getReqd ( config.optString ( "deployApi", "deployApi" ), FlowControlDeploymentService.class );
		fConfigTransfer = sc.getReqd ( config.optString ( "configTransfer", "configTransfer" ), ConfigTransferService.class );

		final CHttpService http = sc.getReqd ( config.getString ( "httpService" ), CHttpService.class );

		http.addRouteInstaller ( new TypicalApiServiceRouteInstaller ()
			.registerRoutes ( "flowControlRoutes.conf", getClass(), new FlowControlRoutes<Identity> ( sc, config, this ) )
			.registerRoutes ( "configFetch.conf", getClass(), new ConfigFetch<Identity> ( sc, config, fConfigTransfer ) )
			.registerErrorHandler ( FlowControlApi.FlowControlApiException.class, HttpStatusCodes.k503_serviceUnavailable )
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
	private final ConfigTransferService fConfigTransfer;
}
