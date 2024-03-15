package io.continual.services.model.client;

import io.continual.services.model.core.data.ModelObject;
import io.continual.util.naming.Path;

public interface ModelObjectWriter
{
	Path getId ();

	void serializeTo ( ModelObject dataWriter );
}
