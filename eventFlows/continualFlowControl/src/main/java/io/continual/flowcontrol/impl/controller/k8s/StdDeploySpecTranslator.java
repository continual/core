package io.continual.flowcontrol.impl.controller.k8s;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.controlapi.FlowControlDeploymentService.DeploymentSpec;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.templating.ContinualTemplateContext;

public class StdDeploySpecTranslator extends SimpleService implements DeploySpecTranslator
{
	public StdDeploySpecTranslator ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
	}

	public void populate ( DeploymentSpec ds, ContinualTemplateContext intoContext )
	{
		intoContext
			.put ( "FC_INSTANCE_COUNT", "" + ds.getInstanceCount () )
			.put ( "FC_STORAGE_SIZE", ds.getResourceSpecs().persistDiskSize () )
			.put ( "FC_LOGS_SIZE", ds.getResourceSpecs().logDiskSize () )
		;
	}
}
