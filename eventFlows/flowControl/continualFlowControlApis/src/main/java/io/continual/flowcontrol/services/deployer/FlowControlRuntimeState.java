package io.continual.flowcontrol.services.deployer;

import java.util.Set;

public interface FlowControlRuntimeState
{
	enum DeploymentStatus
	{
		PENDING,
		RUNNING,
		STOPPING,
		STOPPED,
		FAILED,
		UNKNOWN
	};

	/**
	 * Get the deployment this runtime state captures 
	 * @return the deployment
	 */
	FlowControlDeployment getDeployment ();

	/**
	 * Get the status of this deployment
	 * @return a status
	 */
	DeploymentStatus getStatus ();

	/**
	 * Get the set of deployed processes
	 * @return a set of process IDs
	 */
	Set<String> getProcesses ();

	/**
	 * Get a process instance by its process ID
	 * @param processId
	 * @return process info or null if not found
	 */
	FlowControlDeployedProcess getProcess ( String processId );
}
