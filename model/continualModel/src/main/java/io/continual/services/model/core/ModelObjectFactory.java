package io.continual.services.model.core;

import io.continual.services.model.core.data.ModelDataObjectAccess;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.util.naming.Path;

public interface ModelObjectFactory<T>
{
	T create ( Path path, ModelObjectMetadata metadata, ModelDataObjectAccess data ) throws ModelRequestException;
}
