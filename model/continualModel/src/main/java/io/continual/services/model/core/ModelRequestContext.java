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
 * A single user operation often requires multiple interactions with the model. The model request context 
 * caches information across these interactions. It also provides references for the user's identity, the
 * schema registry and the notification service.
 */
public interface ModelRequestContext extends AutoCloseable
{
	/**
	 * Get the operator for this context.
	 * @return the operator
	 */
	Identity getOperator ();

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

	/**
	 * CacheControl signals what kind of caching operation is allowed on the transaction
	 */
	enum CacheControl
	{
		NONE,
		READ_NO_WRITE,
		WRITE_NO_READ,
		READ_AND_WRITE
	};

	/**
	 * Get the cache control setting
	 * @return cache control setting
	 */
	CacheControl getCacheControl ();

	/**
	 * Get a previously stored raw data.
	 *  
	 * @param key
	 * @return the raw data, if previously loaded (and stored here). Otherwise, null.
	 * @throws ClassCastException
	 */
	<T> T get ( Path key, Class<T> clazz );

	/**
	 * Put an object into the context
	 * @param key the object path
	 * @param rawData the raw data
	 */
	void put ( Path key, Object rawData );

	/**
	 * Remove an object a given path.
	 * @param objectPath the object path
	 */
	void remove ( Path objectPath );

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
}
