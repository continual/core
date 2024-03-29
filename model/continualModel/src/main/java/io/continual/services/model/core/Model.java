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

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.identity.Identity;
import io.continual.services.Service;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelSchemaViolationException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.naming.Path;

public interface Model extends ModelIdentification, ModelCapabilities, Closeable, Service
{
	/**
	 * A builder for request contexts.
	 */
	interface ModelRequestContextBuilder
	{
		/**
		 * Associated an identity in this model request context
		 * @param user
		 * @return this context
		 */
		ModelRequestContextBuilder forUser ( Identity user );

		ModelRequestContextBuilder withSchemasFrom ( ModelSchemaRegistry reg );

		ModelRequestContextBuilder withNotificationsTo ( ModelNotificationService notifications );

		/**
		 * Build the request context
		 * @return a request context
		 * @throws BuildFailure
		 */
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
	 * List paths immediately below the given path.
	 * @param context
	 * @param parentPath
	 * @return a list of 0 or more object paths
	 * @throws ModelServiceException
	 * @throws ModelItemDoesNotExistException
	 * @throws ModelRequestException
	 */
	ModelPathList listChildrenOfPath ( ModelRequestContext context, Path parentPath ) throws ModelServiceException, ModelItemDoesNotExistException, ModelRequestException;

	/**
	 * Start a query on this model implementation.
	 * @return a query 
	 * @throws ModelRequestException 
	 * @throws ModelServiceException 
	 */
	ModelQuery startQuery () throws ModelRequestException, ModelServiceException;

	/**
	 * Start a traversal on this model implementation.
	 * @return a traversal
	 * @throws ModelRequestException 
	 */
	ModelTraversal startTraversal () throws ModelRequestException;

	/**
	 * Create an index on a given field for use in later queries.
	 * @param field A dot-notation field name.
	 * @return this model
	 */
	Model createIndex ( String field ) throws ModelRequestException, ModelServiceException;
	
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
	@Deprecated
	default Model store ( ModelRequestContext context, Path objectPath, String jsonData ) throws ModelRequestException, ModelSchemaViolationException, ModelServiceException
	{
		createUpdate ( context, objectPath )
			.overwrite ( JsonUtil.readJsonObject ( jsonData ) )
			.execute ()
		;
		return this;
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
	@Deprecated
	default Model store ( ModelRequestContext context, Path objectPath, JSONObject jsonData ) throws ModelRequestException, ModelSchemaViolationException, ModelServiceException
	{
		createUpdate ( context, objectPath )
			.overwrite ( jsonData )
			.execute ()
		;
		return this;
	}

	/**
	 * Merge the given JSON into the object at the given path. If the object doesn't exist, it's created.
	 * @param context
	 * @param objectPath
	 * @param jsonData
	 * @throws ModelRequestException
	 * @throws ModelSchemaViolationException
	 * @throws ModelServiceException
	 */
	@Deprecated
	default Model update ( ModelRequestContext context, Path objectPath, JSONObject jsonData ) throws ModelRequestException, ModelSchemaViolationException, ModelServiceException
	{
		createUpdate ( context, objectPath )
			.merge ( jsonData )
			.execute ()
		;
		return this;
	}

	/**
	 * An object updater
	 */
	interface ObjectUpdater
	{
		ObjectUpdater overwrite ( JSONObject withData );

		ObjectUpdater merge ( JSONObject withData );

		ObjectUpdater replaceAcl ( AccessControlList acl );

		void execute () throws ModelRequestException, ModelSchemaViolationException, ModelServiceException;
	};

	/**
	 * Begin an update
	 * @param context
	 * @param objectPath
	 * @return an object updater
	 * @throws ModelRequestException
	 * @throws ModelServiceException
	 */
	ObjectUpdater createUpdate ( ModelRequestContext context, Path objectPath ) throws ModelRequestException, ModelServiceException;
	
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
	 * RelationType 
	 */
	enum RelationType
	{
		UNORDERED,	// default
		ORDERED,
	}
	
	/**
	 * Set the relation type. 
	 * @param context
	 * @param relnName
	 * @param rt
	 * @return this model
	 */
	Model setRelationType ( ModelRequestContext context, String relnName, RelationType rt ) throws ModelServiceException, ModelRequestException;
	
	/**
	 * Relate two objects with a relationship in the graph. If the relation already exists, the request has no effect. If the relation
	 * is ordered, the new relation instance becomes last in the ordered list of related objects.
	 * 
	 * @param context
	 * @param reln
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	ModelRelationInstance relate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException;

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
	 * Remove a relation between two objects.
	 * @param context
	 * @param reln
	 * @return true if the relationship existed 
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	default boolean unrelate ( ModelRequestContext context, ModelRelationInstance reln ) throws ModelServiceException, ModelRequestException
	{
		return unrelate ( context, reln.getId () );
	}

	/**
	 * Remove a relation between two objects by relation ID
	 * @param context
	 * @param relnId
	 * @return true if the relationship existed 
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	boolean unrelate ( ModelRequestContext context, String relnId ) throws ModelServiceException, ModelRequestException;

	/**
	 * Relation Selector
	 */
	interface RelationSelector 
	{
		/**
		 * Select relations with any name
		 * @return this selector
		 */
		default RelationSelector withAnyName () { return named ( null ); }

		/**
		 * Select relations with the given name only
		 * @param name Use null to mean any name
		 * @return this selector
		 */
		RelationSelector named ( String name );

		/**
		 * Select inbound relations only
		 * @return this selector
		 */
		default RelationSelector inboundOnly () { return inbound(true).outbound(false); } 

		/**
		 * Select inbound relations if the parameter is true 
		 * @param wantInbound
		 * @return this selector
		 */
		RelationSelector inbound ( boolean wantInbound );

		/**
		 * Select outbound relations only
		 * @return this selector
		 */
		default RelationSelector outboundOnly () { return inbound(false).outbound(true); }

		/**
		 * Select outbound relations if the parameter is true
		 * @param wantOutbound
		 * @return this selector
		 */
		RelationSelector outbound ( boolean wantOutbound );

		/**
		 * Select both inbound and outbound relations
		 * @return this selector
		 */
		default RelationSelector inboundAndOutbound () { return inbound(true).outbound (true); }

		/**
		 * Get the requested relations
		 * @param context
		 * @return a model relation list
		 * @throws ModelServiceException
		 * @throws ModelRequestException
		 */
		ModelRelationList getRelations ( ModelRequestContext context ) throws ModelServiceException, ModelRequestException;
	};
	
	/**
	 * Start a relations query. The default selector returns all relations in both directions.
	 * @param objectPath
	 * @return a relation selector
	 */
	RelationSelector selectRelations ( Path objectPath );
}
