package io.continual.flowcontrol.impl.k8s;

import java.io.IOException;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.FlowControlApi;
import io.continual.flowcontrol.FlowControlCallContext;
import io.continual.flowcontrol.FlowControlCallContextBuilder;
import io.continual.flowcontrol.FlowControlService;
import io.continual.flowcontrol.impl.common.BaseHttpService;
import io.continual.services.ServiceContainer;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;

public class K8sFlowControlService extends BaseHttpService implements FlowControlService
{
	public K8sFlowControlService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config );

		try
		{
			fClient = Config.defaultClient ();
			fNamespace = config.optString ( "namespace", "default" );

			Configuration.setDefaultApiClient ( fClient );
		}
		catch ( IOException e )
		{
			throw new BuildFailure ( e );
		}
	}
	
	@Override
	public FlowControlCallContextBuilder createtContextBuilder ()
	{
		return new K8sFlowControlCallContext.Builder ( fClient, fNamespace );
	}

	@Override
	public FlowControlApi getApiFor ( FlowControlCallContext ctx )
	{
		// if this cast doesn't work, it's an error on the caller's part
		return new K8sFlowControlApi ( (K8sFlowControlCallContext) ctx );
	}

	private final String fNamespace;
	private final ApiClient fClient;
}
