package io.continual.flowcontrol.services.deploydb;

import java.util.List;

import io.continual.flowcontrol.model.FlowControlDeployment;
import io.continual.iam.identity.Identity;
import io.continual.services.Service;

/**
 * A deployment DB stores information about active deployments. 
 */
public interface DeploymentDb extends Service
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
	void storeDeployment ( FlowControlDeployment deployment ) throws DeployDbException;

	/**
	 * Remove the given deployment
	 * @param deployId
	 */
	void removeDeployment ( String deployId ) throws DeployDbException;

	/**
	 * Load a deployment based on its ID
	 * @param deployId
	 * @return a deployment or null
	 */
	FlowControlDeployment getDeploymentById ( String deployId ) throws DeployDbException;

	/**
	 * Load all deployments for a given user
	 * @param userId
	 * @return a list of 0 or more deployments
	 */
	List<FlowControlDeployment> getDeploymentsForUser ( Identity userId ) throws DeployDbException;

	/**
	 * Load all deployments for a given job
	 * @param jobId
	 * @return a list of 0 or more deployments
	 */
	List<FlowControlDeployment> getDeploymentsOfJob ( String jobId ) throws DeployDbException;

	/**
	 * Load a deployment by config key
	 * @param configKey
	 * @return a deployment or null
	 */
	FlowControlDeployment getDeploymentByConfigKey ( String configKey ) throws DeployDbException;
}
