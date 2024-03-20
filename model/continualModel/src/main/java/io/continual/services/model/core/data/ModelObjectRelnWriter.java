package io.continual.services.model.core.data;

import io.continual.util.naming.Path;

public interface ModelObjectRelnWriter
{
	enum Direction
	{
		INBOUND,
		OUTBOUND
	};

	/**
	 * Serializer for a given named relation
	 */
	interface NamedRelnSerializer
	{
		/**
		 * This set will overwrite the set in the model
		 * @return this serializer
		 */
		NamedRelnSerializer asOverwrite ();

		/**
		 * This set will be appended to the set in the model
		 * @return this serializer
		 */
		NamedRelnSerializer asAppend ();

		/**
		 * Add the remote object to this set.
		 * @param remoteObject
		 * @return this serializer
		 */
		NamedRelnSerializer add ( Path remoteObject );

		/**
		 * Remove the remote object from the set. This is only applicable in 
		 * "append" mode and is ignored in "overwrite" mode.
		 * @param remoteObject
		 * @return this serializer
		 */
		NamedRelnSerializer remove ( Path remoteObject );
	}

	/**
	 * Get the specific relation set to work with
	 * @param name the name of the relation
	 * @param dir the direction relative to the object being serialized
	 * @return a named relation serializer
	 */
	NamedRelnSerializer getRelation ( String name, Direction dir );
}
