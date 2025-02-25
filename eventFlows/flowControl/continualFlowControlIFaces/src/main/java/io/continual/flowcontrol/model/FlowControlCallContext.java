package io.continual.flowcontrol.model;

import io.continual.iam.identity.Identity;

/**
 * A context for making calls into the flow control service.
 */
public interface FlowControlCallContext
{
	/**
	 * Get the user making the flow control call
	 * @return a user record
	 */
	Identity getUser ();
}
