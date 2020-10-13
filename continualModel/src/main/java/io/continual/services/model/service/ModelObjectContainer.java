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

package io.continual.services.model.service;

import io.continual.iam.access.ProtectedResource;
import io.continual.iam.identity.Identity;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelServiceAccessException;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.util.naming.Name;

/**
 * A model object container is a model object (represented as JSON) as well as a protected
 * resource. Additionally, it contains 0 or more child containers.
 */
public interface ModelObjectContainer extends ModelObject, ProtectedResource
{
	/**
	 * Determine whether a user can perform a given operation.
	 * @param user
	 * @param op
	 * @return true if the user can perform the operation, false otherwise
	 */
	boolean canUser ( Identity user, ModelOperation op );

	/**
	 * Check if a user can perform a given operation. If not, ModelServiceAccessException is thrown
	 * @param user
	 * @param op
	 * @throws ModelServiceAccessException
	 * @throws ModelServiceIoException
	 */
	void checkUser ( Identity user, ModelOperation op ) throws ModelServiceAccessException, ModelServiceIoException;

	/**
	 * Check if a child object exists
	 * @param context
	 * @param itemName
	 * @return true if the child exists
	 * @throws ModelServiceIoException 
	 * @throws ModelServiceRequestException 
	 */
	boolean exists ( ModelRequestContext context, Name itemName ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Load an object contained by this one.
	 * @param context
	 * @param itemName The name of the child object.
	 * @return a model object
	 */
	ModelObjectContainer load ( ModelRequestContext context, Name itemName ) throws ModelItemDoesNotExistException, ModelServiceRequestException, ModelServiceIoException;

	/**
	 * Store an object as a child of this one with the given name.
	 * @param context
	 * @param itemName
	 * @param o
	 * @throws ModelServiceAccessException 
	 * @throws ModelServiceRequestException 
	 */
	void store ( ModelRequestContext context, Name itemName, ModelObject o ) throws ModelServiceIoException, ModelServiceAccessException, ModelServiceRequestException;

	/**
	 * Get the elements below the given path.
	 * @param context
	 * @return a list of child elements
	 * @throws ModelServiceRequestException 
	 * @throws ModelServiceIoException 
	 */
	ModelElementList getElementsBelow ( ModelRequestContext context ) throws ModelServiceRequestException, ModelServiceIoException;

	/**
	 * Remove the given child item from this object.
	 * @param itemName
	 * @return true if the item existed and was removed
	 * @throws ModelServiceAccessException 
	 * @throws ModelServiceIoException 
	 * @throws ModelServiceRequestException 
	 */
	boolean remove ( ModelRequestContext context, Name itemName ) throws ModelServiceIoException, ModelServiceAccessException, ModelServiceRequestException;
}
