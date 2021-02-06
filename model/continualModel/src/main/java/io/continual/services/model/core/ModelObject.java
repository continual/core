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

import java.util.Set;

import org.json.JSONObject;

import io.continual.iam.access.ProtectedResource;

/**
 * A model object is data that can be represented as JSON, is a protected resource
 * in the system, and is subject to type-locking.
 */
public interface ModelObject extends ProtectedResource
{
	/**
	 * Get the set of locked type names for this object
	 * @return a set of 0 or more type names
	 */
	Set<String> getTypes ();

	/**
	 * Get a JSON representation of this object.
	 * @return a JSON string
	 */
	String asJson ();

	JSONObject getData ();
}
