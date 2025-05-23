package io.continual.flowcontrol.model;

import java.util.List;

import io.continual.services.Service;

/**
 * A deployment DB stores information about active deployments. 
 */
public interface FlowControlDeploymentDb extends Service
{
	class DeployDbException extends Exception
	{
		public DeployDbException ( String msg ) { super(msg); };
		public DeployDbException ( Throwable t ) { super(t); };
		public DeployDbException ( String msg, Throwable t ) { super(msg,t); };
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Store the given deployment
	 * @param deployment
	 */
	void storeDeployment ( FlowControlDeploymentRecord deployment ) throws DeployDbException;

	/**
	 * Remove the given deployment
	 * @param deployId
	 * @return the deployment record if it existed
	 */
	FlowControlDeploymentRecord removeDeployment ( String deployId ) throws DeployDbException;

	/**
	 * Load a deployment based on its ID
	 * @param deployId
	 * @return a deployment or null
	 */
	FlowControlDeploymentRecord getDeploymentById ( String deployId ) throws DeployDbException;

	/**
	 * Load all deployments for a given user
	 * @param fccc
	 * @return a list of 0 or more deployments
	 */
	List<FlowControlDeploymentRecord> getDeploymentsForUser ( FlowControlCallContext fccc ) throws DeployDbException;

	/**
	 * Load all deployments for a given job
	 * @param jobId
	 * @return a list of 0 or more deployments
	 */
	List<FlowControlDeploymentRecord> getDeploymentsOfJob ( String jobId ) throws DeployDbException;

	/**
	 * Load a deployment by config key
	 * @param configKey
	 * @return a deployment or null
	 */
	FlowControlDeploymentRecord getDeploymentByConfigKey ( String configKey ) throws DeployDbException;
}
