package io.continual.flowcontrol.model;

import io.continual.iam.identity.Identity;

/**
 * A flow control call context builder
 */
public interface FlowControlCallContextBuilder
{
	/**
	 * Set the user for the context
	 * @param i
	 * @return this builder
	 */
	FlowControlCallContextBuilder asUser ( Identity i );

	/**
	 * Build the context
	 * @return a context
	 */
	FlowControlCallContext build ();
}
