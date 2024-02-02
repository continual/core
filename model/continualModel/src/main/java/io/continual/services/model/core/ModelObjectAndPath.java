package io.continual.services.model.core;

import io.continual.util.naming.Path;

public interface ModelObjectAndPath
{
	Path getPath ();

	ModelObject getObject ();

	static ModelObjectAndPath from ( final Path p, final ModelObject o )
	{
		return new ModelObjectAndPath ()
		{
			@Override
			public Path getPath () { return p; }

			@Override
			public ModelObject getObject () { return o; }
		};
	}
}
