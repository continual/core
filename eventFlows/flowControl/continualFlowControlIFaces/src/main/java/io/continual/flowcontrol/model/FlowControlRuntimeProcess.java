package io.continual.flowcontrol.model;

import java.util.List;

import io.continual.metrics.MetricsCatalog;

/**
 * A process within a deployment
 */
public interface FlowControlRuntimeProcess
{
	/**
	 * Get the ID for the deployment.
	 * @return an ID
	 */
	String getProcessId ();

	// TBD: process status? mapping from things like k8s PodStatus
	
	/**
	 * Get a log listing for this instance
	 * @param sinceRfc3339Time a time to use as "since", can be null
	 * @return a list of text entries
	 */
	List<String> getLog ( String sinceRfc3339Time ) throws FlowControlDeploymentService.ServiceException, FlowControlDeploymentService.RequestException;

	/**
	 * Get metrics associated with this process
	 * @return a metrics catalog for this process
	 */
	MetricsCatalog getMetrics ();
}
