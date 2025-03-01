package io.continual.flowcontrol.model;

import io.continual.flowcontrol.model.FlowControlDeploymentService.ServiceException;
import io.continual.services.Service;

/**
 * A runtime environment reports information about running deployments.
 */
public interface FlowControlRuntimeSystem extends Service
{
	/**
	 * Get the running process information
	 * @param fccc the call context
	 * @param deploymentId the ID of a deployment
	 * @return a runtime state
	 * @throws ServiceException 
	 */
	FlowControlRuntimeState getRuntimeState ( FlowControlCallContext fccc, String deploymentId ) throws ServiceException;
}
