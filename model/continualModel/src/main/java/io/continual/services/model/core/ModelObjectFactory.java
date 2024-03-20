package io.continual.services.model.core;

import io.continual.services.model.core.data.ModelObject;
import io.continual.services.model.core.exceptions.ModelRequestException;

public interface ModelObjectFactory<T,K>
{
	interface ObjectCreateContext<K>
	{
		ModelObjectMetadata getMetadata ();
		ModelObject getData ();
		K getUserContext ();
	};

	T create ( ObjectCreateContext<K> context ) throws ModelRequestException;
}
