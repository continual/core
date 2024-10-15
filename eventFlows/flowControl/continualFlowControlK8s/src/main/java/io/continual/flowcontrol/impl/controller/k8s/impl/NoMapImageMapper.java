package io.continual.flowcontrol.impl.controller.k8s.impl;

import io.continual.flowcontrol.impl.controller.k8s.ContainerImageMapper;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlRuntimeSpec;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.RequestException;

public class NoMapImageMapper implements ContainerImageMapper
{
	@Override
	public String getImageName ( FlowControlRuntimeSpec rs ) throws RequestException
	{
		return rs.getName () + ":" + rs.getVersion ();
	}
}
