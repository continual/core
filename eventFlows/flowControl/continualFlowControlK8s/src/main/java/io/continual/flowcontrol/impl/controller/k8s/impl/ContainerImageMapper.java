package io.continual.flowcontrol.impl.controller.k8s.impl;

import io.continual.flowcontrol.model.FlowControlDeploymentService.RequestException;
import io.continual.flowcontrol.model.FlowControlDeploymentService.ServiceException;
import io.continual.flowcontrol.model.FlowControlJob.FlowControlRuntimeSpec;

/**
 * The container image mapper maps from a runtime specification to a container image name.
 * 
 * (FIXME: this is k8s specific stuff that should be in the k8s impl)
 */
public interface ContainerImageMapper
{
	/**
	 * Return a container image spec (e.g. "package:version") given a runtime spec.
	 * @param runtimeSpec
	 * @return a string suitable as a container name
	 * @throws RequestException 
	 * @throws ServiceException 
	 */
	String getImageName ( FlowControlRuntimeSpec runtimeSpec ) throws RequestException, ServiceException;
}
