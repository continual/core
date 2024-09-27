package io.continual.flowcontrol.impl.controller.k8s;

import io.continual.flowcontrol.model.FlowControlJob.FlowControlRuntimeSpec;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.RequestException;

/**
 * The container image mapper maps from a runtime specification to a container image name.
 */
public interface ContainerImageMapper
{
	/**
	 * Return a container image spec (e.g. "package:version") given a runtime spec.
	 * @param runtimeSpec
	 * @return a string suitable as a container name
	 * @throws RequestException 
	 */
	String getImageName ( FlowControlRuntimeSpec runtimeSpec ) throws RequestException;
}
