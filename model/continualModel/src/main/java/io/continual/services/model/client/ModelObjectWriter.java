package io.continual.services.model.client;

import io.continual.services.model.core.data.ModelObject;

public interface ModelObjectWriter
{
	void serializeTo ( ModelObject dataWriter );
}
