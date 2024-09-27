package io.continual.flowcontrol.impl.controller.k8s;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.model.FlowControlDeploymentSpec;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.templating.ContinualTemplateContext;

/**
 * Standard deployment spec translator populates various values into the template context, including
 * FC_INSTANCE_COUNT, FC_STORAGE_SIZE, and FC_LOGS_SIZE.
 */
public class StdDeploySpecTranslator extends SimpleService implements DeploySpecTranslator
{
	public StdDeploySpecTranslator ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
	}

	@Override
	public void populate ( FlowControlDeploymentSpec ds, ContinualTemplateContext intoContext )
	{
		intoContext
			.put ( "FC_INSTANCE_COUNT", "" + ds.getInstanceCount () )
			.put ( "FC_STORAGE_SIZE", ds.getResourceSpecs().persistDiskSize () )
			.put ( "FC_LOGS_SIZE", ds.getResourceSpecs().logDiskSize () )
		;
	}
}
