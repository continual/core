package io.continual.flowcontrol.model;

import io.continual.iam.access.ProtectedResource;
import io.continual.iam.identity.Identity;

/**
 * A deployment is a record of deploying a specific deployment specification.
 */
public interface FlowControlDeployment extends ProtectedResource
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
	 * Get a signed configuration key that identifies this deployment. This string is typically
	 * used by worker nodes to fetch the configuration they're intended to run. It should be treated
	 * like a bearer token -- it's enough by itself to pull config data which may include secrets.
	 * @return a config key
	 */
	String getConfigToken ();

	/**
	 * Get the flow control deployment spec used to create this deployment
	 * @return a flow control deployment spec
	 */
	FlowControlDeploymentSpec getDeploymentSpec ();
}
