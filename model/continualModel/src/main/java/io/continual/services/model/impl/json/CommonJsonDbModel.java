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
import java.util.LinkedList;
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
import io.continual.services.model.core.ModelObjectFactory;
import io.continual.services.model.core.ModelObjectMetadata;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelSchema;
import io.continual.services.model.core.ModelSchema.ValidationResult;
import io.continual.services.model.core.ModelSchemaRegistry;
import io.continual.services.model.core.ModelTraversal;
import io.continual.services.model.core.data.BasicModelObject;
import io.continual.services.model.core.data.ModelObject;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelSchemaViolationException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.BasicModelRequestContextBuilder;
import io.continual.services.model.impl.common.SimpleTraversal;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.naming.Path;

public abstract class CommonJsonDbModel extends SimpleService implements Model
{
	public CommonJsonDbModel ( ServiceContainer sc, JSONObject config )
	{
		this ( config.getString ( "modelId" ), config.optBoolean ( "readOnly", false ) );
	}

	public CommonJsonDbModel ( String modelId )
	{
		this ( modelId, false );
	}

	public CommonJsonDbModel ( String modelId, boolean readOnly )
	{
		fModelId = modelId;
		fReadOnly = readOnly;
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
	public <T,K> T load ( ModelRequestContext context, Path objectPath, ModelObjectFactory<T,K> factory, K userContext ) throws ModelItemDoesNotExistException, ModelServiceException, ModelRequestException
	{
		if ( context.knownToNotExist ( objectPath ) )
		{
			throw new ModelItemDoesNotExistException ( objectPath );
		}

		ModelDataTransfer ld = context.get ( objectPath, ModelDataTransfer.class );
		if ( ld == null )
		{
			ld = loadObject ( context, objectPath );
			if ( ld == null )
			{
				context.doesNotExist ( objectPath );
				throw new ModelItemDoesNotExistException ( objectPath );
			}
			context.put ( objectPath, ld );
		}

		final ModelDataTransfer ldf = ld;
		final ModelObjectFactory.ObjectCreateContext<K> createContext = new ModelObjectFactory.ObjectCreateContext<K> ()
		{
			@Override
			public ModelObjectMetadata getMetadata () { return ldf.getMetadata (); }

			@Override
			public ModelObject getData () { return ldf.getObjectData (); }

			@Override
			public K getUserContext () { return userContext; }
		};
		
		return factory.create ( createContext );
	}

	@Override
	public ObjectUpdater createUpdate ( final ModelRequestContext context, final Path objectPath ) throws ModelRequestException, ModelServiceException
	{
		checkReadOnly ();

		return new ObjectUpdater ()
		{
			@Override
			public ObjectUpdater overwriteData ( ModelObject withData )
			{
				fUpdates.add ( new Update ( UpdateType.OVERWRITE_DATA, withData ) );
				return this;
			}

			@Override
			public ObjectUpdater mergeData ( ModelObject withData )
			{
				fUpdates.add ( new Update ( UpdateType.MERGE_DATA, withData ) );
				return this;
			}

			@Override
			public ObjectUpdater replaceAcl ( AccessControlList acl )
			{
				fUpdates.add ( new Update ( acl ) );
				return this;
			}

			@Override
			public ObjectUpdater addTypeLock ( String typeId )
			{
				fUpdates.add ( new Update ( UpdateType.ADD_TYPE_LOCK, typeId ) );
				return this;
			}

			@Override
			public ObjectUpdater removeTypeLock ( String typeId )
			{
				fUpdates.add ( new Update ( UpdateType.REMOVE_TYPE_LOCK, typeId ) );
				return this;
			}

			@Override
			public void execute () throws ModelRequestException, ModelSchemaViolationException, ModelServiceException
			{
				try
				{
					final boolean isCreate = !exists ( context, objectPath );

					final ModelObjectMetadata meta;
					final CommonObjectData data;

					if ( isCreate )
					{
						meta = new CommonModelObjectMetadata ();

						final Identity id = context.getOperator ();
						if ( id != null )
						{
							meta.getAccessControlList ()
								.setOwner ( id.getId () )
								.permit ( AccessControlEntry.kOwner, ModelOperation.kAllOperationStrings )
							;
						}
						else
						{
							meta.getAccessControlList ()
								.addAclEntry ( AccessControlEntry.builder ().permit ().forAllUsers ().forAnyOperation ().build () )
							;
						}
						data = new CommonObjectData ();
					}
					else
					{
						final BasicModelObject o = load ( context, objectPath );
						meta = o.getMetadata ();
						data = new CommonObjectData ( o.getData () );
					}

					for ( Update mu : fUpdates )
					{
						final AccessControlList acl = meta.getAccessControlList ();
						final ModelOperation[] accessList = mu.getAccessRequired ();
						for ( ModelOperation access : accessList )
						{
							final Identity id = context.getOperator ();
							if ( !acl.canUser ( id, access.toString () ) )
							{
								final String userId = id == null ? "Anonymous" : id.getId ();
								throw new ModelRequestException ( userId + " may not " + access + " " + objectPath.toString () + "." );
							}
						}
						mu.update ( context, meta, data );
					}

					// validate the update
					final ModelSchemaRegistry schemas = context.getSchemaRegistry ();
					for ( String type : meta.getLockedTypes () )
					{
						final ModelSchema ms = schemas.getSchema ( type );
						if ( ms == null )
						{
							throw new ModelRequestException ( "Unknown type " + type );
						}
						final ValidationResult vr = ms.isValid ( data );
						if ( !vr.isValid () )
						{
							throw new ModelRequestException ( "The object does not meet type " + type,
								new JSONObject ()
									.put ( "validationProblems", JsonVisitor.listToArray ( vr.getProblems () ) )
							);
						}
					}

					final ModelDataTransfer mdt = new ModelDataTransfer ()
					{
						@Override
						public ModelObjectMetadata getMetadata () { return meta; }

						@Override
						public ModelObject getObjectData () { return data; }
					};
					internalStore ( context, objectPath, mdt );
					log.info ( "wrote {}", objectPath );
					context.put ( objectPath, mdt );

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
			}

			private final LinkedList<Update> fUpdates = new LinkedList<> ();
		};
	}

	private enum UpdateType
	{
		OVERWRITE_DATA,
		MERGE_DATA,
		ACL,
		ADD_TYPE_LOCK,
		REMOVE_TYPE_LOCK
	}

	private class Update
	{
		public void update ( ModelRequestContext context, ModelObjectMetadata meta, CommonObjectData workingData )
		{
			switch ( fType )
			{
				case ACL:
					final AccessControlList targetAcl = meta.getAccessControlList ();
					targetAcl.clear ();
					targetAcl.setOwner ( fAcl.getOwner () );
					for ( AccessControlEntry e : fAcl.getEntries () )
					{
						targetAcl.addAclEntry ( e );
					}					
					break;

				case OVERWRITE_DATA:
					workingData.clear ();
					// no break...

				case MERGE_DATA:
					workingData.merge ( fData );
					break;

				case ADD_TYPE_LOCK:
					meta.getLockedTypes ().add ( fTypeId );
					break;

				case REMOVE_TYPE_LOCK:
					meta.getLockedTypes ().remove ( fTypeId );
					break;

				default:
					throw new RuntimeException ( "Unknown update type." );
			}
		}

		public ModelOperation[] getAccessRequired ()
		{
			if ( fType == UpdateType.ACL )
			{
				return new ModelOperation[] { ModelOperation.ACL_UPDATE };
			}
			else
			{
				return new ModelOperation[] { ModelOperation.UPDATE };
			}
		}

		private Update ( AccessControlList acl )
		{
			fType = UpdateType.ACL;
			fData = null;
			fTypeId = null;
			fAcl = acl;
		}

		private Update ( UpdateType ut, ModelObject data )
		{
			fType = ut;
			fData = data;
			fTypeId = null;
			fAcl = null;
		}

		private Update ( UpdateType ut, String typeId )
		{
			fType = ut;
			fData = null;
			fTypeId = typeId;
			fAcl = null;
		}

		public final UpdateType fType;
		public final ModelObject fData;
		public final AccessControlList fAcl;
		public final String fTypeId;
	}

	@Override
	public boolean remove ( ModelRequestContext context, Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		checkReadOnly ();

		final boolean result = internalRemove ( context, objectPath );
		context.remove ( objectPath );
		log.info ( "removed {}", objectPath );
		context.getNotificationService().onObjectDelete ( objectPath );
		return result;
	}

	@Override
	public Model createIndex ( String field ) throws ModelRequestException, ModelServiceException
	{
		checkReadOnly ();
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

	private final String fModelId;
	private final boolean fReadOnly;

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

	protected interface ModelDataTransfer
	{
		ModelObjectMetadata getMetadata ();
		ModelObject getObjectData ();
	}
	
	protected abstract ModelDataTransfer loadObject ( ModelRequestContext context, Path objectPath ) throws ModelItemDoesNotExistException, ModelServiceException, ModelRequestException;
	protected abstract void internalStore ( ModelRequestContext context, Path objectPath, ModelDataTransfer o ) throws ModelRequestException, ModelServiceException;
	protected abstract boolean internalRemove ( ModelRequestContext context, Path objectPath ) throws ModelRequestException, ModelServiceException;

	@Deprecated
	public static final String kMetadataTag = "Ⓜ";
	@Deprecated
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

	protected void checkReadOnly () throws ModelRequestException
	{
		if ( fReadOnly ) throw new ModelRequestException ( "This model was loaded as read-only." );
	}
}
