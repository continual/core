package io.continual.flowcontrol.impl.controller.k8s;

import io.continual.flowcontrol.controlapi.FlowControlDeploymentService.RequestException;
import io.continual.flowcontrol.controlapi.FlowControlRuntimeSpec;

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
