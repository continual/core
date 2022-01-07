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

import org.json.JSONObject;

import io.continual.iam.access.ProtectedResource;
import io.continual.util.data.json.JsonSerialized;

/**
 * A model object is data that can be represented as JSON, is a protected resource
 * in the system, and is subject to type-locking.
 */
public interface ModelObject extends ProtectedResource, JsonSerialized
{
	/**
	 * Get this object's metadata
	 * @return object metadata
	 */
	ModelObjectMetadata getMetadata ();

	/**
	 * Get a copy of the data in this object
	 * @return a JSON object
	 */
	JSONObject getData ();

	/**
	 * Replace the data in this object with the given data.
	 * @param data
	 */
	void putData ( JSONObject data );
	
	/**
	 * Path the data in this object with the given data.
	 * @param data
	 */
	void patchData ( JSONObject data );
}
