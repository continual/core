package io.continual.flowcontrol.impl.k8s;

import io.continual.flowcontrol.FlowControlJob;
import io.kubernetes.client.openapi.models.V1Pod;

class K8sJob implements FlowControlJob
{
	public K8sJob ( V1Pod item )
	{
		fItem = item;
	}

	@Override
	public String getName ()
	{
		return fItem.getMetadata ().getName ();
	}

	private final V1Pod fItem;
}
