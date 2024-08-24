package io.continual.flowcontrol;

import io.continual.iam.identity.Identity;

public interface FlowControlCallContext
{
	Identity getUser ();
}
