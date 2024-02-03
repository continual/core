package io.continual.services.model.impl.delegator;

import io.continual.services.model.core.Model;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.naming.Path;

/**
 * A model mount specification.
 */
public interface ModelMount extends JsonSerialized
{
	/**
	 * Get the location for this model in the delegating model container
	 * @return a model mount point
	 */
	Path getMountPoint ();

	/**
	 * Does this model contain the given path?
	 * @param path
	 * @return true if this model contains the given path
	 */
	boolean contains ( Path path );

	/**
	 * Get the associated model.
	 * @return a model
	 */
	Model getModel ();

	/**
	 * Given an absolute path, return the same path relative to the model mount point.
	 * @param absolutePath
	 * @return the path within the mounted model
	 */
	Path getPathWithinModel ( Path absolutePath );

	/**
	 * Given an internal path, return the global path within the delegating model.
	 * @param from
	 * @return the path within the global delegating model
	 */
	Path getGlobalPath ( Path from );
}
