package io.continual.flowcontrol.impl.controller.k8s.impl;

import io.continual.flowcontrol.model.FlowControlDeploymentService.RequestException;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlRuntimeSpec;

public class NoMapImageMapper implements ContainerImageMapper
{
	public NoMapImageMapper ()
	{
	}

	@Override
	public String getImageName ( FlowControlRuntimeSpec rs ) throws RequestException
	{
		return rs.getName () + ":" + rs.getVersion ();
	}
}
