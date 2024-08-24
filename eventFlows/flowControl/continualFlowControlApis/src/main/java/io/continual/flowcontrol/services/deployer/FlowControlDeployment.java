package io.continual.flowcontrol.services.deployer;

import java.util.Set;

import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.DeploymentSpec;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.RequestException;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.ServiceException;
import io.continual.iam.identity.Identity;

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
	 * Get the identity that deployed this deployment
	 * @return an identity
	 */
	Identity getDeployer ();
	
	/**
	 * Get a signed configuration key that identifies this deployment
	 * @return a config key
	 */
	String getConfigKey ();
	
	/**
	 * Get the job this deployment is running
	 * @return a job ID
	 */
	String getJobId ();

	/**
	 * Get the flow control deployment spec used to create this deployment
	 * @return a flow control deployment spec
	 */
	DeploymentSpec getDeploymentSpec ();

	/**
	 * Get the status of the given deployment
	 * @return a status
	 */
	Status getStatus () throws FlowControlDeploymentService.ServiceException;

	/**
	 * Get the deployment's instance count. Note that this is the requested count, not
	 * necessarily the deployed count.
	 * @return an instance count
	 */
	int instanceCount ();

	/**
	 * Get the instance names in this deployment
	 * @return a set of instance names
	 */
	Set<String> instances ();

	/**
	 * Get a process instance by its name
	 * @param instanceName
	 * @return a process or null
	 * @throws ServiceException 
	 * @throws RequestException 
	 */
	FlowControlDeployedProcess getProcessById ( String instanceName ) throws RequestException, ServiceException;
}
