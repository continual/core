package io.continual.flowcontrol.services.deployer;

import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.DeploymentSpec;
import io.continual.iam.identity.Identity;

public interface FlowControlDeployment
{
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
	 * Get the flow control deployment spec used to create this deployment
	 * @return a flow control deployment spec
	 */
	DeploymentSpec getDeploymentSpec ();
}
