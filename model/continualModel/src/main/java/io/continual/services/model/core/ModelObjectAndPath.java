package io.continual.services.model.core;

import io.continual.util.naming.Path;

public interface ModelObjectAndPath<T>
{
	Path getPath ();

	T getObject ();

	static <T> ModelObjectAndPath<T> from ( final Path p, final T o )
	{
		return new ModelObjectAndPath<T> ()
		{
			@Override
			public Path getPath () { return p; }

			@Override
			public T getObject () { return o; }
		};
	}
}
