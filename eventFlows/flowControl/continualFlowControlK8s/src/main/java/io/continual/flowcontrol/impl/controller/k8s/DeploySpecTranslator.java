package io.continual.flowcontrol.impl.controller.k8s;

import io.continual.flowcontrol.model.FlowControlDeploymentSpec;
import io.continual.templating.ContinualTemplateContext;

public interface DeploySpecTranslator
{
	void populate ( FlowControlDeploymentSpec ds, ContinualTemplateContext templateCtx );
}
