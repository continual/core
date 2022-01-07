package io.continual.flowcontrol.svcapi;

import io.continual.flowcontrol.FlowControlCallContext;
import io.continual.flowcontrol.FlowControlCallContextBuilder;
import io.continual.iam.identity.Identity;

class BaseFlowControlCallContext implements FlowControlCallContext
{
	public static class Builder implements FlowControlCallContextBuilder
	{
		public Builder ( )
		{
		}
		
		@Override
		public FlowControlCallContextBuilder asUser ( Identity i )
		{
			fUser = i;
			return this;
		}

		@Override
		public FlowControlCallContext build ()
		{
			return new BaseFlowControlCallContext ( this );
		}
		
		private Identity fUser;
	}

	@Override
	public Identity getUser ()
	{
		return fUser;
	}

	private BaseFlowControlCallContext ( Builder b )
	{
		fUser = b.fUser;
	}

	private final Identity fUser;
}
