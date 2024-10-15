package io.continual.flowcontrol.impl.controller.k8s.impl;

import io.continual.flowcontrol.impl.controller.k8s.ContainerImageMapper;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlRuntimeSpec;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.RequestException;

public class ModelBackedImageMapper implements ContainerImageMapper
{
	public ModelBackedImageMapper ()
	{
		
	}

	@Override
	public String getImageName ( FlowControlRuntimeSpec runtimeSpec ) throws RequestException
	{
		throw new RequestException ( "Couldn't map runtime specification " + runtimeSpec.toString () + " to a container image." );
	}
}
