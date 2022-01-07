package io.continual.services.model.core;

public interface ModelUpdater
{
	ModelOperation[] getAccessRequired ();

	void update ( ModelRequestContext context, ModelObject o );
}
