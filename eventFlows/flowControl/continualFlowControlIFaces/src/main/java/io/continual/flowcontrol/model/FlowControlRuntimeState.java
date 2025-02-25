package io.continual.flowcontrol.model;

import java.util.Set;

/**
 * A report on the runtime state of a deployment.
 */
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
	FlowControlRuntimeProcess getProcess ( String processId );
}
