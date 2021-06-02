package io.continual.flowcontrol.impl.k8s;

import java.util.Collection;
import java.util.LinkedList;

import io.continual.flowcontrol.FlowControlApi;
import io.continual.flowcontrol.FlowControlJob;
import io.continual.flowcontrol.FlowControlJobBuilder;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;

class K8sFlowControlApi implements FlowControlApi
{
	public K8sFlowControlApi ( K8sFlowControlCallContext ctx )
	{
		fContext = ctx;
	}
	
	@Override
	public FlowControlJobBuilder createJobBuilder ()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FlowControlApi registerJob ( FlowControlJob job )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<FlowControlJob> getAllJobs () throws FlowControlApiException
	{
		try
		{
			final LinkedList<FlowControlJob> jobs = new LinkedList<> ();

			final CoreV1Api api = new CoreV1Api ();
			final V1PodList list = api.listNamespacedPod ( fContext.getNamespace (), null, null, null, null, null, null, null, null, null, null );
			for ( V1Pod item : list.getItems () )
			{
				jobs.add ( new K8sJob ( item ) );
			}

			return jobs;
		}
		catch ( ApiException e )
		{
			throw new FlowControlApi.FlowControlApiException ( e );
		}
	}

	@Override
	public FlowControlJob getJob ( String name ) throws FlowControlApiException
	{
		try
		{
			final CoreV1Api api = new CoreV1Api ();
			final V1Pod pod = api.readNamespacedPod ( name, fContext.getNamespace (), null, null, null );
			return new K8sJob ( pod );
		}
		catch ( ApiException e )
		{
			throw new FlowControlApi.FlowControlApiException ( e );
		}
	}

	@Override
	public FlowControlApi updateJob ( FlowControlJob job )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FlowControlApi removeJob ( FlowControlJob job )
	{
		// TODO Auto-generated method stub
		return null;
	}

	private final K8sFlowControlCallContext fContext;
}
