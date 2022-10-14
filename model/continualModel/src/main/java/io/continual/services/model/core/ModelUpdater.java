package io.continual.services.model.core;

/**
 * An object that updates an object from a model.
 */
public interface ModelUpdater
{
	/**
	 * Get the operations required for this update.
	 * @return an array of operations
	 */
	ModelOperation[] getAccessRequired ();

	/**
	 * Update the given object.
	 * @param context
	 * @param o
	 */
	void update ( ModelRequestContext context, ModelObject o );
}
