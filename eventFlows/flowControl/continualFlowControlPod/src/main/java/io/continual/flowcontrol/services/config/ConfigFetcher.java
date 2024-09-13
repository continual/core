package io.continual.flowcontrol.services.config;

import java.io.IOException;

import io.continual.flowcontrol.model.FlowControlDeploymentSpec;

/**
 * Configuration fetch interface for worker processes. This interface is expected to be used
 * ahead of a processing run (e.g. in an init container in k8s) to retrieve the event processing
 * system's configuration and any other required resources.
 * 
 * The deployment process includes using the deployment spec to build a working environment with
 * appropriate resource sizing, etc. and the correct runtime engine. The engine image itself is 
 * neutral, so any on-disk resources, such as large configuration files or plug-in libraries,
 * must be fetched during startup. This service provides the ability for a startup process to 
 * retrieve process-specific information.
 */
public interface ConfigFetcher
{
	class ConfigFormatException extends Exception
	{
		public ConfigFormatException ( Throwable t ) { super(t); }
		private static final long serialVersionUID = 1L;
	}
	
	FlowControlDeploymentSpec fetchDeployment ( String configToken ) throws IOException, ConfigFormatException;
}
