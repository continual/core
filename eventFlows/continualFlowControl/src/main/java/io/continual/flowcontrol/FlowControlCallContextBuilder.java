package io.continual.flowcontrol;

import io.continual.iam.identity.Identity;

public interface FlowControlCallContextBuilder
{
	FlowControlCallContextBuilder asUser ( Identity i );

	FlowControlCallContext build ();
}
