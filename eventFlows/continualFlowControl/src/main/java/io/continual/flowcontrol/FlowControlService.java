package io.continual.flowcontrol;

import io.continual.flowcontrol.controlapi.FlowControlDeploymentService;
import io.continual.flowcontrol.jobapi.FlowControlJobDb;
import io.continual.services.Service;

/**
 * The flow control service is a controller for event processors.
 * 
 */
public interface FlowControlService extends Service
{
	FlowControlCallContextBuilder createtContextBuilder ();

	FlowControlJobDb getJobDb ( FlowControlCallContext fccc );

	FlowControlDeploymentService getDeployer ( FlowControlCallContext fccc );
}
