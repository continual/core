package io.continual.services.processor.config.readers;

import java.util.List;

import io.continual.services.ServiceContainer;

public interface ConfigLoadContext
{
	ServiceContainer getServiceContainer ();

	List<String> getSearchPathPackages ();
}
