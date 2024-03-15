package io.continual.services.model.core;

import io.continual.services.model.core.data.ModelObject;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.util.naming.Path;

public interface ModelObjectFactory<T,K>
{
	interface ObjectCreateContext<K>
	{
		Path getPath ();
		ModelObjectMetadata getMetadata ();
		ModelObject getData ();
		K getUserContext ();
	};

	T create ( ObjectCreateContext<K> context ) throws ModelRequestException;
}
