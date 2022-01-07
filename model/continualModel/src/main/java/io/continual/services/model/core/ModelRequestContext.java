/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.services.model.core;

import io.continual.iam.identity.Identity;
import io.continual.util.naming.Path;

/**
 * The model request context is created once per user request. It caches data
 * used during request handling.
 */
public interface ModelRequestContext
{
	enum CacheControl
	{
		NONE,
		READ_NO_WRITE,
		WRITE_NO_READ,
		READ_AND_WRITE
	};
	
	/**
	 * Get the operator for this context.
	 * @return the operator
	 */
	Identity getOperator ();

	/**
	 * Get the cache control settings
	 * @return cache control settings
	 */
	CacheControl getCacheControl ();

	/**
	 * Get a previously loaded object from this context.
	 *  
	 * @param key
	 * @return the object, if previously loaded (and stored here). Otherwise, null.
	 */
	ModelObject get ( Path key );

	/**
	 * Put an object into the context
	 * @param key the object path
	 * @param o the object
	 */
	void put ( Path key, ModelObject o );

	/**
	 * Remove an object a given path.
	 * @param objectPath the object path
	 */
	void remove ( Path objectPath );

	/**
	 * Get the named data from the context.
	 * @param key the raw data key
	 * @return the data, or null
	 */
	Object getRawData ( String key );

	/**
	 * Get raw data as the given type.
	 * @param key the raw data key
	 * @param c the class to cast the raw data to
	 * @return the raw data stored with the key, cast to the given type, or null
	 */
	<T> T getRawData ( String key, Class<T> c );

	/**
	 * Put the named data and value into the context
	 * @param key the raw data key
	 * @param value a raw data object
	 */
	void putRawData ( String key, Object value );

	/**
	 * Tell the context that a key does not exist in the model.
	 * @param key the object path
	 */
	void doesNotExist ( Path key );

	/**
	 * Check if the key is known to not exist (from a prior call to doesNotExist())
	 * @param key the object path
	 * @return true if the key is known to not exist
	 */
	boolean knownToNotExist ( Path key );

	/**
	 * Where is this model mounted in the user's global name system?
	 * @return a Path
	 */
	Path getMountPoint ();

	/**
	 * Get the schema registry
	 * @return a schema registry
	 */
	ModelSchemaRegistry getSchemaRegistry ();

	/**
	 * Get notification service
	 * @return a notification service
	 */
	ModelNotificationService getNotificationService ();
}
