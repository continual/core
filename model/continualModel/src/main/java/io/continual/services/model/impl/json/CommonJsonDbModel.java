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

package io.continual.services.model.impl.json;

import java.io.IOException;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.iam.access.AccessControlEntry;
import io.continual.iam.access.AccessControlList;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelNotificationService;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelSchema;
import io.continual.services.model.core.ModelSchema.ValidationResult;
import io.continual.services.model.core.ModelSchemaRegistry;
import io.continual.services.model.core.ModelTraversal;
import io.continual.services.model.core.ModelUpdater;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.BasicModelRequestContextBuilder;
import io.continual.services.model.impl.common.SimpleTraversal;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.naming.Path;

public abstract class CommonJsonDbModel extends SimpleService implements Model
{
	public CommonJsonDbModel ( ServiceContainer sc, JSONObject config )
	{
		this ( config.getString ( "acctId" ), config.getString ( "modelId" ) );
	}

	public CommonJsonDbModel ( String acctId, String modelId )
	{
		fAcctId = acctId;
		fModelId = modelId;
	}

	@Override
	public void close () throws IOException
	{
	}

	@Override
	public ModelRequestContextBuilder getRequestContextBuilder ()
	{
		return new BasicModelRequestContextBuilder ();
	}

	@Override
	public String getAcctId ()
	{
		return fAcctId;
	}

	@Override
	public String getId ()
	{
		return fModelId;
	}

	@Override
	public long getMaxSerializedObjectLength ()
	{
		// arbitrary default limit - 1GB
		return 1024L * 1024L * 1024L;
	}

	@Override
	public long getMaxPathLength ()
	{
		// arbitrary default limit - 1KB
		return 1024L;
	}

	@Override
	public long getMaxRelnNameLength ()
	{
		// arbitrary default limit - 1KB
		return 1024L;
	}

	@Override
	public boolean exists ( ModelRequestContext context, Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		if ( context.knownToNotExist ( objectPath ) ) return false;

		final boolean result = objectExists ( context, objectPath );
		if ( !result )
		{
			context.doesNotExist ( objectPath );
		}
		return result;
	}

	@Override
	public ModelObject load ( ModelRequestContext context, Path objectPath ) throws ModelItemDoesNotExistException, ModelServiceException, ModelRequestException
	{
		if ( context.knownToNotExist ( objectPath ) )
		{
			throw new ModelItemDoesNotExistException ( objectPath );
		}

		ModelObject result = context.get ( objectPath );
		if ( result != null ) return result;

		result = loadObject ( context, objectPath );
		if ( result == null )
		{
			context.doesNotExist ( objectPath );
			throw new ModelItemDoesNotExistException ( objectPath );
		}
		else
		{
			context.put ( objectPath, result );
		}
		return result;
	}

	@Override
	public Model store ( ModelRequestContext context, Path objectPath, ModelUpdater... updates ) throws ModelRequestException, ModelServiceException
	{
		try
		{
			final boolean isCreate = !exists ( context, objectPath );

			ModelObject o;
			if ( isCreate )
			{
				o = initializeObject ( context );
			}
			else
			{
				o = load ( context, objectPath );
			}

			for ( ModelUpdater mu : updates )
			{
				final AccessControlList acl = o.getAccessControlList ();
				final ModelOperation[] accessList = mu.getAccessRequired ();
				for ( ModelOperation access : accessList )
				{
					if ( !acl.canUser ( context.getOperator (), access.toString () ) )
					{
						throw new ModelRequestException ( context.getOperator ().getId () + " may not " + access + " " + objectPath.toString () + "." );
					}
				}
				mu.update ( context, o );
			}

			// validate the update
			final ModelSchemaRegistry schemas = context.getSchemaRegistry ();
			for ( String type : o.getMetadata ().getLockedTypes () )
			{
				final ModelSchema ms = schemas.getSchema ( type );
				if ( ms == null )
				{
					throw new ModelRequestException ( "Unknown type " + type );
				}
				final ValidationResult vr = ms.isValid ( o );
				if ( !vr.isValid () )
				{
					throw new ModelRequestException ( "The object does not meet type " + type,
						new JSONObject ()
							.put ( "validationProblems", JsonVisitor.listToArray ( vr.getProblems () ) )
					);
				}
			}

			internalStore ( context, objectPath, o );
			log.info ( "wrote {}", objectPath );
			context.put ( objectPath, o );

			final ModelNotificationService ns = context.getNotificationService();
			if ( isCreate ) 
			{
				ns.onObjectCreate ( objectPath );
			}
			else
			{
				ns.onObjectUpdate ( objectPath );
			}
		}
		catch ( IamSvcException e )
		{
			throw new ModelServiceException ( e );
		}
		return this;
	}

	@Override
	public boolean remove ( ModelRequestContext context, Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		final boolean result = internalRemove ( context, objectPath );
		context.remove ( objectPath );
		log.info ( "removed {}", objectPath );
		context.getNotificationService().onObjectDelete ( objectPath );
		return result;
	}

	@Override
	public Model createIndex ( String field ) throws ModelRequestException, ModelServiceException
	{
		return this;
	}

	@Override
	public ModelTraversal startTraversal () throws ModelRequestException
	{
		return new SimpleTraversal ( this );
	}

	@Override
	public RelationSelector selectRelations ( Path objectPath )
	{
		return new CommonRelationSelector ( this, objectPath );
	}

	private final String fAcctId;
	private final String fModelId;

	protected ModelObject initializeObject ( ModelRequestContext context ) throws ModelRequestException
	{
		final Identity id = context.getOperator ();
		if ( id == null )
		{
			throw new ModelRequestException ( "No operator identity provided in request context." );
		}

		final CommonJsonDbObject result = new CommonJsonDbObject ();
		result.getAccessControlList ()
			.setOwner ( id.getId () )
			.permit ( AccessControlEntry.kOwner, ModelOperation.kAllOperationStrings )
		;
		return result;
	}

	protected boolean objectExists ( ModelRequestContext context, Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		try
		{
			load ( context, objectPath );
			return true;
		}
		catch ( ModelItemDoesNotExistException e )
		{
			return false;
		}
	}

	protected abstract ModelObject loadObject ( ModelRequestContext context, Path objectPath ) throws ModelItemDoesNotExistException, ModelServiceException, ModelRequestException;
	protected abstract void internalStore ( ModelRequestContext context, Path objectPath, ModelObject o ) throws ModelRequestException, ModelServiceException;
	protected abstract boolean internalRemove ( ModelRequestContext context, Path objectPath ) throws ModelRequestException, ModelServiceException;

	public static final String kMetadataTag = "Ⓜ";
	public static final String kUserDataTag = "Ⓤ";

	/**
	 * Get inbound related objects with a given name from a given object
	 * @param context
	 * @param forObject
	 * @param named send null to retrieve any
	 * @return a list of 0 or more relations, with getTo set to forObject and getName set to named
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	@Deprecated
	public abstract List<ModelRelationInstance> getInboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException;

	/**
	 * Get outbound related objects with a given name from a given object
	 * @param context
	 * @param forObject
	 * @param named
	 * @return a list of 0 or more relations, with getFrom set to forObject and getName set to named
	 * @throws ModelServiceException
	 * @throws ModelRequestException
	 */
	@Deprecated
	public abstract List<ModelRelationInstance> getOutboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException;

	private static final Logger log = LoggerFactory.getLogger ( CommonJsonDbModel.class );
}
