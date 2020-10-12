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

import java.util.Collection;
import java.util.List;

import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectUpdater;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.naming.Path;

/**
 * A model is a collection of objects and relationships (and eventually we'll
 * restore types).
 */
public interface Model extends ModelObjectContainer, JsonSerialized
{
	/**
	 * Get this model's name in the context of the containing account.
	 */
	String getId ();
	
	/**
	 * Does the given path exist?
	 * @param context
	 * @param id
	 * @return true if the path exists (for this user)
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	boolean exists ( ModelRequestContext context, Path objectPath ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Load an object
	 * @param context
	 * @param id
	 * @return an object
	 * @throws ModelItemDoesNotExistException
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	ModelObjectContainer load ( ModelRequestContext context, Path objectPath ) throws ModelItemDoesNotExistException, ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Store the given object at the given path
	 * @param context
	 * @param id
	 * @param o
	 * @param metadata
	 * @throws ModelServiceRequestException
	 * @throws ModelServiceIoException
	 */
	void store ( ModelRequestContext context, Path objectPath, ModelObject o ) throws ModelServiceRequestException, ModelServiceIoException;

	/**
	 * Store the given JSON as an object at the given path
	 * @param context
	 * @param id
	 * @param jsonData
	 * @param metadata
	 * @throws ModelServiceRequestException
	 * @throws ModelServiceIoException
	 */
	void store ( ModelRequestContext context, Path objectPath, String jsonData ) throws ModelServiceRequestException, ModelServiceIoException;

	/**
	 * Update an existing object. If the object doesn't exist, it's created.
	 * @param context
	 * @param id
	 * @param o the object update tool
	 * @param metadata
	 * @throws ModelServiceRequestException
	 * @throws ModelServiceIoException
	 */
	void update ( ModelRequestContext context, Path objectPath, ModelObjectUpdater o ) throws ModelServiceRequestException, ModelServiceIoException;

	/**
	 * Remove (delete) an object from the model
	 * @param context
	 * @param id
	 * @return true if and only if an object was removed
	 * @throws ModelServiceRequestException 
	 * @throws ModelServiceIoException 
	 */
	boolean remove ( ModelRequestContext context, Path objectPath ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Relate two objects with a relationship in the graph. If the relation already exists, the request has no effect.
	 * @param reln
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	void relate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Relate two objects with a relationship in the graph. If the relation already exists, the request has no effect.
	 * @param relns
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	void relate ( ModelRequestContext context, Collection<ModelRelation> relns ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Remove a relation between two objects.
	 * @param reln
	 * @return true if the relationship existed 
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	boolean unrelate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Get all related objects from a given object
	 * @param forObject
	 * @return a list of 0 or more relations
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	List<ModelRelation> getRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Get inbound related objects from a given object
	 * @param forObject
	 * @return a list of 0 or more relations, with getTo set to forObject
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	List<ModelRelation> getInboundRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Get outbound related objects from a given object
	 * @param forObject
	 * @return a list of 0 or more relations, with getFrom set to forObject
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	List<ModelRelation> getOutboundRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Get inbound related objects with a given name from a given object
	 * @param forObject
	 * @param named
	 * @return a list of 0 or more relations, with getTo set to forObject and getName set to named
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	List<ModelRelation> getInboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Get outbound related objects with a given name from a given object
	 * @param forObject
	 * @param named
	 * @return a list of 0 or more relations, with getFrom set to forObject and getName set to named
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	List<ModelRelation> getOutboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceIoException, ModelServiceRequestException;
}
