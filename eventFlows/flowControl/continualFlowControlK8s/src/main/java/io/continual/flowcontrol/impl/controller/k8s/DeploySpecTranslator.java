package io.continual.flowcontrol.impl.controller.k8s;

import io.continual.flowcontrol.model.FlowControlDeploymentSpec;
import io.continual.templating.ContinualTemplateContext;

/**
 * The deployment spec translator populates a template context with data from a deployment spec.
 */
public interface DeploySpecTranslator
{
	/**
	 * Populate the given template context based on data in the given deployment spec
	 * @param ds
	 * @param templateCtx
	 */
	void populate ( FlowControlDeploymentSpec ds, ContinualTemplateContext templateCtx );
}
