package io.continual.services.model.impl.delegator;

import io.continual.services.model.core.Model;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.naming.Path;

public interface ModelMount extends JsonSerialized
{
	Path getMountPoint ();

	boolean contains ( Path path );

	Model getModel ();

	Path getPathWithinModel ( Path absolutePath );

	Path getGlobalPath ( Path from );
}
