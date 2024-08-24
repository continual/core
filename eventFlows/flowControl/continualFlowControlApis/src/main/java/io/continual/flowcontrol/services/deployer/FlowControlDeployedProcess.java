package io.continual.flowcontrol.services.deployer;

import java.util.List;

import io.continual.metrics.MetricsCatalog;

/**
 * A process within a deployment
 */
public interface FlowControlDeployedProcess
{
	/**
	 * Get the ID for the deployment.
	 * @return an ID
	 */
	String getProcessId ();

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
