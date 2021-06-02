package io.continual.flowcontrol.impl.k8s;

import io.continual.flowcontrol.FlowControlCallContext;
import io.continual.flowcontrol.FlowControlCallContextBuilder;
import io.continual.iam.identity.Identity;
import io.kubernetes.client.openapi.ApiClient;

class K8sFlowControlCallContext implements FlowControlCallContext
{
	public static class Builder implements FlowControlCallContextBuilder
	{
		public Builder ( ApiClient api, String ns )
		{
			fApiClient = api;
			fNamespace = ns;
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
			return new K8sFlowControlCallContext ( this );
		}
		
		private final ApiClient fApiClient;
		private final String fNamespace;

		private Identity fUser;
	}

	@Override
	public Identity getUser ()
	{
		return fUser;
	}

	public ApiClient getK8sApiClient () { return fApiClient; }
	public String getNamespace () { return fNamespace; }
	
	private K8sFlowControlCallContext ( Builder b )
	{
		fUser = b.fUser;
		fApiClient = b.fApiClient;
		fNamespace = b.fNamespace;
	}

	private final Identity fUser;
	private final ApiClient fApiClient;
	private final String fNamespace;
}
