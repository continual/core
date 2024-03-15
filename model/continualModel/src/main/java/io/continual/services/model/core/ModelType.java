package io.continual.services.model.core;

import io.continual.services.model.core.data.ModelObject;

/**
 * An object type
 */
public interface ModelType
{
	/**
	 * Get the ID of this type.
	 * @return a string ID
	 */
	String getId ();

	/**
	 * Does the given object comply with this type?
	 * @param obj
	 * @return true if the object complies to this type spec
	 */
	boolean isA ( ModelObject obj );
}
