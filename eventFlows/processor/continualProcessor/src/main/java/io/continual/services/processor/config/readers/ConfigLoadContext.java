package io.continual.services.processor.config.readers;

import java.util.List;

import io.continual.services.ServiceContainer;

public interface ConfigLoadContext
{
	/**
	 * Get the service container for this configuration load
	 * @return a service container
	 */
	ServiceContainer getServiceContainer ();

	/**
	 * Get the search path packages used for this configuration load
	 * @return a list of 0 or more search paths
	 */
	List<String> getSearchPathPackages ();
}
