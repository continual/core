package io.continual.flowcontrol.controlapi;

import java.util.List;
import java.util.Set;

public interface FlowControlDeployment
{
	enum Status
	{
		PENDING,
		RUNNING,
		STOPPING,
		STOPPED,
		FAILED,
		UNKNOWN
	}

	/**
	 * Get the deployment ID
	 * @return a deployment ID
	 */
	String getId ();

	/**
	 * Get the job this deployment is running
	 * @return a job ID
	 */
	String getJobId ();

	/**
	 * Get the status of the given deployment
	 * @return a status
	 */
	Status getStatus () throws FlowControlDeploymentService.ServiceException;

	/**
	 * Get the deployment's instance count
	 * @return an instance count
	 */
	int instanceCount ();
	
	/**
	 * Get the instance names in this deployment
	 * @return a set of instance names
	 */
	Set<String> instances ();

	/**
	 * Get a log listing for a given instance
	 * @param instanceId
	 * @param sinceRfc3339Time a time to use as "since", can be null
	 * @return a list of text entries
	 */
	List<String> getLog ( String instanceId, String sinceRfc3339Time ) throws FlowControlDeploymentService.ServiceException, FlowControlDeploymentService.RequestException;

	/**
	 * Get the pod ID for the deployment based on its 0-indexed instance number.
	 * @param instanceNo
	 * @return a pod ID
	 */
	String getPodId ( int instanceNo );
}
