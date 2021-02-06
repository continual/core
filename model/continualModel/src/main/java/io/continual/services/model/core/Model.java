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

import java.util.Collection;
import java.util.List;

import io.continual.iam.identity.Identity;
import io.continual.services.Service;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.service.ModelRelation;
import io.continual.util.naming.Path;

public interface Model extends Service
{
	/**
	 * Get the ID of the account that owns this model.
	 * @return the account ID
	 */
	String getAcctId ();

	/**
	 * Get this model's name in the context of the containing account.
	 */
	String getId ();

	/**
	 * A builder for request contexts.
	 */
	interface ModelRequestContextBuilder
	{
		ModelRequestContextBuilder forUser ( Identity user );
		ModelRequestContext build ();
	};

	/**
	 * Get a request context builder
	 * @return a request context builder
	 */
	ModelRequestContextBuilder getRequestContextBuilder ();

	/**
	 * Get a basic request context.
	 * @return a request context
	 */
	default ModelRequestContext getDefaultContext ( Identity user  )
	{
		return getRequestContextBuilder().forUser ( user ).build ();
	}
	
	/**
	 * Does the given path exist?
	 * @param context
	 * @param objectPath
	 * @return true if the path exists (for this user)
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	boolean exists ( ModelRequestContext context, Path objectPath ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * List objects within the prefix path.
	 * @param context
	 * @param prefix
	 * @return a list of 0 or more object pathss
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	List<Path> listObjectsStartingWith ( ModelRequestContext context, Path prefix ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Query the model for objects that pass each of the given filters.
	 * @param context
	 * @param prefix
	 * @param orderBy If not null, how to order the response
	 * @param filters
	 * @return a list of 0 or more model objects
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	List<ModelObject> queryModelForObjects ( ModelRequestContext context, Path prefix, ModelObjectComparator orderBy, ModelObjectFilter... filters ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Load an object
	 * @param context
	 * @param objectPath
	 * @return an object
	 * @throws ModelItemDoesNotExistException
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	ModelObject load ( ModelRequestContext context, Path objectPath ) throws ModelItemDoesNotExistException, ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Store the given object at the given path
	 * @param context
	 * @param objectPath
	 * @param o
	 * @throws ModelServiceRequestException
	 * @throws ModelServiceIoException
	 */
	void store ( ModelRequestContext context, Path objectPath, ModelObject o ) throws ModelServiceRequestException, ModelServiceIoException;

	/**
	 * Store the given JSON as an object at the given path
	 * @param context
	 * @param objectPath
	 * @param jsonData
	 * @throws ModelServiceRequestException
	 * @throws ModelServiceIoException
	 */
	void store ( ModelRequestContext context, Path objectPath, String jsonData ) throws ModelServiceRequestException, ModelServiceIoException;

	/**
	 * Update an existing object. If the object doesn't exist, it's created.
	 * @param context
	 * @param objectPath
	 * @param o the object update tool
	 * @throws ModelServiceRequestException
	 * @throws ModelServiceIoException
	 */
	void update ( ModelRequestContext context, Path objectPath, ModelObjectUpdater o ) throws ModelServiceRequestException, ModelServiceIoException;

	/**
	 * Remove (delete) an object from the model
	 * @param context
	 * @param objectPath
	 * @return true if and only if an object was removed
	 * @throws ModelServiceRequestException 
	 * @throws ModelServiceIoException 
	 */
	boolean remove ( ModelRequestContext context, Path objectPath ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Relate two objects with a relationship in the graph. If the relation already exists, the request has no effect.
	 * @param context
	 * @param reln
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	void relate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Relate two objects with a relationship in the graph. If the relation already exists, the request has no effect.
	 * @param context
	 * @param relns
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	void relate ( ModelRequestContext context, Collection<ModelRelation> relns ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Remove a relation between two objects.
	 * @param context
	 * @param reln
	 * @return true if the relationship existed 
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	boolean unrelate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Get all related objects from a given object
	 * @param context
	 * @param forObject
	 * @return a list of 0 or more relations
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	List<ModelRelation> getRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Get inbound related objects from a given object
	 * @param context
	 * @param forObject
	 * @return a list of 0 or more relations, with getTo set to forObject
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	List<ModelRelation> getInboundRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Get outbound related objects from a given object
	 * @param context
	 * @param forObject
	 * @return a list of 0 or more relations, with getFrom set to forObject
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	List<ModelRelation> getOutboundRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Get inbound related objects with a given name from a given object
	 * @param context
	 * @param forObject
	 * @param named
	 * @return a list of 0 or more relations, with getTo set to forObject and getName set to named
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	List<ModelRelation> getInboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Get outbound related objects with a given name from a given object
	 * @param context
	 * @param forObject
	 * @param named
	 * @return a list of 0 or more relations, with getFrom set to forObject and getName set to named
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	List<ModelRelation> getOutboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceIoException, ModelServiceRequestException;
}
