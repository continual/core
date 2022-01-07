package io.continual.services.model.core;

import io.continual.services.model.core.exceptions.ModelServiceException;

public interface ModelSchemaRegistry
{
	/**
	 * Get the named schema, or return null if it's not available
	 * @param name
	 * @return a schema or null
	 * @throws ModelServiceException 
	 */
	ModelSchema getSchema ( String name ) throws ModelServiceException;
}
