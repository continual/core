package io.continual.flowcontrol.impl.controller.k8s;

import io.continual.flowcontrol.controlapi.FlowControlDeploymentService.DeploymentSpec;
import io.continual.templating.ContinualTemplateContext;

public interface DeploySpecTranslator
{
	void populate ( DeploymentSpec ds, ContinualTemplateContext templateCtx );
}
