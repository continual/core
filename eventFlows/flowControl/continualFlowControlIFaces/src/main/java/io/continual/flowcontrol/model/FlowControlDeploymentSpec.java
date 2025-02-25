package io.continual.flowcontrol.model;

import java.util.Map;

/**
 * A deployment specification that includes a job, an instance count, an environment, and 
 * applicable resource specifications.
 */
public interface FlowControlDeploymentSpec
{
	/**
	 * The job to deploy
	 * @return the job
	 */
	FlowControlJob getJob ();

	/**
	 * The number of instances to deploy
	 * @return a number, min 1
	 */
	int getInstanceCount ();

	/**
	 * The process environment to provide in the deployment
	 * @return a map of keys/values
	 */
	Map<String,String> getEnv ();

	/**
	 * Get resource specs. This result is always non-null.
	 * @return a resource spec
	 */
	FlowControlDeploymentResourceSpec getResourceSpecs ();
}
