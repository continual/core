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

import java.io.Closeable;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.identity.Identity;
import io.continual.services.Service;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelSchemaViolationException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.core.updaters.DataOverwrite;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.naming.Path;

public interface Model extends ModelIdentification, ModelCapabilities, Closeable, Service
{
	/**
	 * A builder for request contexts.
	 */
	interface ModelRequestContextBuilder
	{
		ModelRequestContextBuilder forUser ( Identity user );

		ModelRequestContextBuilder mountedAt ( Path mountPoint );

		ModelRequestContextBuilder withSchemasFrom ( ModelSchemaRegistry reg );

		ModelRequestContextBuilder withNotificationsTo ( ModelNotificationService notifications );

		ModelRequestContext build () throws BuildFailure;
	};

	/**
	 * Get a request context builder
	 * @return a request context builder
	 */
	ModelRequestContextBuilder getRequestContextBuilder ();

	/**
	 * Does the given path exist?
	 * @param context
	 * @param objectPath
	 * @return true if the path exists (for this user)
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	boolean exists ( ModelRequestContext context, Path objectPath ) throws ModelServiceException, ModelRequestException;

	/**
	 * List objects within the prefix path.
	 * @param context
	 * @param prefix
	 * @return a list of 0 or more object paths, or null if the requested prefix path does not exist
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	ModelPathList listObjectsStartingWith ( ModelRequestContext context, Path prefix ) throws ModelServiceException, ModelRequestException;

	/**
	 * Start a query on this model implementation.
	 * @return a query 
	 * @throws ModelRequestException 
	 */
	ModelQuery startQuery () throws ModelRequestException;
	
	/**
	 * Load an object. For models that use a container (directory) structure, a path to a container should return an ObjectContainer object.
	 * @param context
	 * @param objectPath
	 * @return an object
	 * @throws ModelItemDoesNotExistException
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	ModelObject load ( ModelRequestContext context, Path objectPath ) throws ModelItemDoesNotExistException, ModelServiceException, ModelRequestException;

	/**
	 * Store the given JSON as an object at the given path
	 * @param context
	 * @param objectPath
	 * @param jsonData
	 * @throws ModelRequestException
	 * @throws ModelSchemaViolationException
	 * @throws ModelServiceException
	 */
	default void store ( ModelRequestContext context, Path objectPath, String jsonData ) throws ModelRequestException, ModelSchemaViolationException, ModelServiceException
	{
		store ( context, objectPath, new DataOverwrite ( JsonUtil.readJsonObject ( jsonData ) ) );
	}

	/**
	 * Store the given JSON as an object at the given path
	 * @param context
	 * @param objectPath
	 * @param jsonData
	 * @throws ModelRequestException
	 * @throws ModelSchemaViolationException
	 * @throws ModelServiceException
	 */
	default void store ( ModelRequestContext context, Path objectPath, JSONObject jsonData ) throws ModelRequestException, ModelSchemaViolationException, ModelServiceException
	{
		store ( context, objectPath, new DataOverwrite ( jsonData ) );
	}

	/**
	 * Update an existing object. If the object doesn't exist, it's created. 
	 * @param context
	 * @param objectPath
	 * @param updates the updaters
	 * @throws ModelRequestException
	 * @throws ModelSchemaViolationException
	 * @throws ModelServiceException
	 */
	void store ( ModelRequestContext context, Path objectPath, ModelUpdater... updates ) throws ModelRequestException, ModelSchemaViolationException, ModelServiceException;

	/**
	 * Remove (delete) an object from the model
	 * @param context
	 * @param objectPath
	 * @return true if and only if an object was removed
	 * @throws ModelRequestException 
	 * @throws ModelServiceException 
	 */
	boolean remove ( ModelRequestContext context, Path objectPath ) throws ModelServiceException, ModelRequestException;

	/**
	 * Relate two objects with a relationship in the graph. If the relation already exists, the request has no effect.
	 * @param context
	 * @param reln
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	void relate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException;

	/**
	 * Remove a relation between two objects.
	 * @param context
	 * @param reln
	 * @return true if the relationship existed 
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	boolean unrelate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException;

	/**
	 * Get all related objects from a given object
	 * @param context
	 * @param forObject
	 * @return a list of 0 or more relations
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	default List<ModelRelation> getRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelation> result = new LinkedList<> ();
		result.addAll ( getInboundRelations ( context, forObject ) );
		result.addAll ( getOutboundRelations ( context, forObject ) );
		return result;
	}

	/**
	 * Get inbound related objects from a given object
	 * @param context
	 * @param forObject
	 * @return a list of 0 or more relations, with getTo set to forObject
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	default List<ModelRelation> getInboundRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceException, ModelRequestException
	{
		return getInboundRelationsNamed ( context, forObject, null );
	}

	/**
	 * Get outbound related objects from a given object
	 * @param context
	 * @param forObject
	 * @return a list of 0 or more relations, with getFrom set to forObject
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	default List<ModelRelation> getOutboundRelations ( ModelRequestContext context, Path forObject ) throws ModelServiceException, ModelRequestException
	{
		return getOutboundRelationsNamed ( context, forObject, null );
	}

	/**
	 * Get inbound related objects with a given name from a given object
	 * @param context
	 * @param forObject
	 * @param named send null to retrieve any
	 * @return a list of 0 or more relations, with getTo set to forObject and getName set to named
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	List<ModelRelation> getInboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException;

	/**
	 * Get outbound related objects with a given name from a given object
	 * @param context
	 * @param forObject
	 * @param named
	 * @return a list of 0 or more relations, with getFrom set to forObject and getName set to named
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	List<ModelRelation> getOutboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException;
}
