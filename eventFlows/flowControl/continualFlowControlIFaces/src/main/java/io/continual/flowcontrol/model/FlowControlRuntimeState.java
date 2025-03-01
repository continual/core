package io.continual.flowcontrol.model;

import java.util.Set;
import java.util.TreeSet;

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

	/**
	 * Create a simple "not deployed" runtime state
	 * @return a runtime state
	 */
	public static FlowControlRuntimeState notRunning ()
	{
		return new FlowControlRuntimeState ()
		{
			@Override
			public DeploymentStatus getStatus () { return DeploymentStatus.STOPPED; }

			@Override
			public Set<String> getProcesses () { return new TreeSet<> (); }

			@Override
			public FlowControlRuntimeProcess getProcess ( String processId ) { return null; }
		};
	}
}
