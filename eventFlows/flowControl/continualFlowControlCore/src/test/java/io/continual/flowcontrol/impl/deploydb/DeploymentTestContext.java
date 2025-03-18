package io.continual.flowcontrol.impl.deploydb;

import io.continual.flowcontrol.model.FlowControlCallContext;
import io.continual.iam.identity.Identity;

/**
 * A test context
 */
public class DeploymentTestContext implements FlowControlCallContext
{
	public DeploymentTestContext ( Identity u ) { fUser = u; }

	@Override
	public Identity getUser () { return fUser; }

	private final Identity fUser;
}
