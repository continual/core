package io.continual.services.model.core;

public interface ModelCapabilities
{
	/**
	 * Get the maximum length of a path in this model.
	 * @return a maximum length for an object's path string
	 */
	long getMaxPathLength ();

	/**
	 * Get the maximum length of a relation name in this model.
	 * @return a max length for a relation name string
	 */
	long getMaxRelnNameLength ();

	/**
	 * Get the maximum serialized object length this model supports.
	 * @return a maximum length for serialized object data
	 */
	long getMaxSerializedObjectLength ();
}
