package io.continual.flowcontrol;

import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.flowcontrol.model.FlowControlCallContextBuilder;
import io.continual.flowcontrol.model.FlowControlJobDb;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService;
import io.continual.services.Service;

/**
 * The flow control service is a controller for event processors.
 */
public interface FlowControlService extends Service
{
	FlowControlCallContextBuilder createtContextBuilder ();

	FlowControlJobDb getJobDb ( FlowControlCallContext fccc );

	FlowControlDeploymentService getDeployer ( FlowControlCallContext fccc );
}
