package io.continual.flowcontrol.model;

import io.continual.services.Service;

/**
 * The flow control service, a controller for event processors.
 */
public interface FlowControlService extends Service
{
	/**
	 * Get a call context builder from the service.
	 * @return a call context builder
	 */
	FlowControlCallContextBuilder createtContextBuilder ();

	/**
	 * Get the job database, which stores flow control jobs on behalf of users.
	 * @param fccc a call context
	 * @return a job database
	 */
	FlowControlJobDb getJobDb ( FlowControlCallContext fccc );

	/**
	 * Get the job deployer, which deploys flow control jobs.
	 * @param fccc a call context
	 * @return a deployment service
	 */
	FlowControlDeploymentService getDeployer ( FlowControlCallContext fccc );

	/**
	 * Get the runtime system, which tracks and reports status of running deployments.
	 * @param fccc a call context
	 * @return the runtime system
	 */
	FlowControlRuntimeSystem getRuntimeSystem ( FlowControlCallContext fccc );
}
