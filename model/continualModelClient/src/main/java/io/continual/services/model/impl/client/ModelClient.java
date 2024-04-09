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

package io.continual.services.model.impl.client;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.access.AccessControlList;
import io.continual.jsonHttpClient.HttpUsernamePasswordCredentials;
import io.continual.jsonHttpClient.JsonOverHttpClient;
import io.continual.jsonHttpClient.JsonOverHttpClient.BodyFormatException;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpRequest;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpResponse;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpServiceException;
import io.continual.jsonHttpClient.impl.ok.OkHttp;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObjectAndPath;
import io.continual.services.model.core.ModelObjectFactory;
import io.continual.services.model.core.ModelObjectFactory.ObjectCreateContext;
import io.continual.services.model.core.ModelObjectList;
import io.continual.services.model.core.ModelObjectMetadata;
import io.continual.services.model.core.ModelPathList;
import io.continual.services.model.core.ModelQuery;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelTraversal;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.data.ModelObject;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelSchemaViolationException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.BasicModelRequestContextBuilder;
import io.continual.services.model.impl.common.SimpleModelQuery;
import io.continual.services.model.impl.common.SimpleTraversal;
import io.continual.services.model.impl.json.CommonDataTransfer;
import io.continual.services.model.impl.json.CommonJsonDbModel;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;
import io.continual.util.naming.Path;

public class ModelClient extends SimpleService implements Model
{
	public ModelClient ( String acctId, String modelId, String baseUrl, Path pathPrefix, String username, String password )
	{
		fAcctId = acctId;
		fModelId = modelId;

		fClient = new OkHttp ();
		fBaseUrl = baseUrl;
		fPathPrefix = pathPrefix;
		fCreds = new HttpUsernamePasswordCredentials ( username, password );
	}

	public ModelClient ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		try
		{
			final ExpressionEvaluator ee = sc.getExprEval ();

			fAcctId = config.getString ( "acctId" );
			fModelId = config.getString ( "modelId" );

			fClient = new OkHttp ();
			fBaseUrl = config.optString ( "baseUrl", "https://model.continual.io" );
			fPathPrefix = Path.fromString ( config.optString ( "pathPrefix", "/" ) );
			fCreds = new HttpUsernamePasswordCredentials (
				ee.evaluateText ( config.getString ( "username" ) ),
				ee.evaluateText ( config.getString ( "password" ) )
			);
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
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
	public void close () throws IOException
	{
		fClient.close ();
	}

	@Override
	public ModelRequestContextBuilder getRequestContextBuilder ()
	{
		return new BasicModelRequestContextBuilder ()
			.forUser ( new LocalIdentity ( fCreds.getUser () ) )
		;
	}

	@Override
	public ModelPathList listChildrenOfPath ( ModelRequestContext context, Path prefix ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<Path> objects = new LinkedList<> ();

        return new ModelPathList ()
        {
			@Override
			public Iterator<Path> iterator ()
			{
				return objects.iterator ();
			}
		};
	}

	@Override
	public ModelQuery startQuery ()
	{
		return new RemoteModelQuery ();
	}

	@Override
	public Model setRelationType ( ModelRequestContext context, String relnName, RelationType rt ) throws ModelServiceException, ModelRequestException
	{
		// FIXME
		throw new ModelRequestException ( "not yet implemented" );
	}

	@Override
	public ModelRelationInstance relate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		final JSONObject relationPayload = new JSONObject ()
			.put ( "relations", new JSONArray ()
				.put ( new JSONObject ()
					.put ( "from", reln.getFrom ().toString () )
					.put ( "name", reln.getName () )
					.put ( "to", reln.getTo ().toString () )
				)
			)
		;

		try ( 
			final HttpResponse resp = fClient.newRequest ()
				.asUser ( fCreds )
				.onPath ( getBasePath ( "relations" ) + "?fail=any" )
				.post ( relationPayload )
		)
		{
			if ( resp.isClientError () )
			{
				throw new ModelRequestException ( "server replied " + resp.getCode () + " " + resp.getMessage () );
			}
			else if ( resp.isServerError () )
			{
				throw new ModelServiceException ( "server replied " + resp.getCode () + " " + resp.getMessage () );
			}

			final JSONObject data = resp.getBody ();
			final JSONArray relns = data.getJSONArray ( "relations" );
			if ( relns.length () != 1 ) throw new BodyFormatException ( "Received " + relns.length () + " relations in response to posting one." );
			final JSONObject newReln = relns.getJSONObject ( 0 );
			final String id = newReln.getString ( "id" );

			return new ModelRelationInstance ()
			{
				@Override
				public String getId () { return id; } 

				@Override
				public Path getFrom () { return reln.getFrom (); }

				@Override
				public Path getTo () { return reln.getTo (); }

				@Override
				public String getName () { return reln.getName (); }

				@Override
				public int compareTo ( ModelRelation that ) { return ModelRelation.compare ( this, that ); }
			};
		}
		catch ( HttpServiceException | BodyFormatException | JSONException e )
		{
			throw new ModelServiceException ( e );
		}
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		throw new ModelServiceException ( "The model service doesn't currently have an API for removing a relation without its ID." );
		
//		final String path = getBasePath ("relations") + "/" + encodePath ( reln.getFrom () ) + "/" + encodeString ( reln.getName () ) + "/" + encodePath ( reln.getTo () );  
//		try ( 
//			final HttpResponse resp = fClient.newRequest ()
//				.asUser ( fCreds )
//				.onPath ( path )
//				.delete ( )
//			)
//		{
//			if ( resp.isSuccess () )
//			{
//				final JSONObject respBody = resp.getBody ();
//				return respBody.optBoolean ( "removal", false );
//			}
//			else if ( resp.isClientError () )
//			{
//				throw new ModelRequestException ( "server replied " + resp.getCode () + " " + resp.getMessage () );
//			}
//			else
//			{
//				throw new ModelServiceException ( "server replied " + resp.getCode () + " " + resp.getMessage () );
//			}
//		}
//		catch ( HttpServiceException | BodyFormatException e )
//		{
//			throw new ModelServiceException ( e );
//		}
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, String relnId ) throws ModelServiceException, ModelRequestException
	{
		try ( 
			final HttpResponse resp = fClient.newRequest ()
				.asUser ( fCreds )
				.onPath ( getBasePath ( "relations" ) + "/" + encodeString ( relnId ) )
				.delete ( )
			)
		{
			if ( resp.isSuccess () )
			{
				final JSONObject respBody = resp.getBody ();
				return respBody.optBoolean ( "removal", false );
			}
			else if ( resp.isClientError () )
			{
				throw new ModelRequestException ( "server replied " + resp.getCode () + " " + resp.getMessage () );
			}
			else
			{
				throw new ModelServiceException ( "server replied " + resp.getCode () + " " + resp.getMessage () );
			}
		}
		catch ( HttpServiceException | BodyFormatException e )
		{
			throw new ModelServiceException ( e );
		}
	}

	public List<ModelRelationInstance> getInboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		return getRelns ( forObject, true, named );
	}

	public List<ModelRelationInstance> getOutboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		return getRelns ( forObject, false, named );
	}


	@Override
	public boolean exists ( ModelRequestContext context, Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		if ( context.knownToNotExist ( objectPath ) ) return false;
		try
		{
			load ( context, objectPath );
			return true;
		}
		catch ( ModelItemDoesNotExistException e )
		{
			context.doesNotExist ( objectPath );
			return false;
		}
	}

	@Override
	public <T,K> T load ( ModelRequestContext context, Path objectPath, ModelObjectFactory<T,K> factory, K userContext ) throws ModelItemDoesNotExistException, ModelServiceException, ModelRequestException
	{
		// check if the cache knows there's no such object
		if ( context.knownToNotExist ( objectPath ) )
		{
			throw new ModelItemDoesNotExistException ( objectPath );
		}

		// check if the cache has the object
		CommonDataTransfer ld = context.get ( objectPath, CommonDataTransfer.class );
		if ( ld == null )
		{
			// otherwise load from server
			final String path = pathToUrl ( objectPath );
			try ( 
				final HttpResponse resp = fClient.newRequest ()
					.asUser ( fCreds )
					.onPath ( path )
					.get ()
				)
			{
				if ( resp.isSuccess () )
				{
					final JSONObject respBody = resp.getBody ();
					final JSONObject obj = respBody.optJSONObject ( "object" );
					if ( obj == null )
					{
						throw new ModelServiceException ( "Expected 'object' in response payload." );
					}
					ld = new CommonDataTransfer ( objectPath, obj );

					context.put ( objectPath, ld );
				}
				else if ( resp.isNotFound () )
				{
					context.doesNotExist ( objectPath );
					throw new ModelItemDoesNotExistException ( objectPath );
				}
				else
				{
					throw new ModelServiceException ( "server replied " + resp.getCode () + " " + resp.getMessage () );
				}
			}
			catch ( HttpServiceException | BodyFormatException e )
			{
				throw new ModelServiceException ( e );
			}
		}

		// now create the instance
		final CommonDataTransfer ldf = ld;
		return factory.create ( new ObjectCreateContext<K> ()
		{
			@Override
			public ModelObjectMetadata getMetadata () { return ldf.getMetadata (); }

			@Override
			public ModelObject getData () { return ldf.getObjectData (); }

			@Override
			public K getUserContext () { return userContext; }
		} );
	}

	@Override
	public ObjectUpdater createUpdate ( ModelRequestContext context, Path objectPath ) throws ModelRequestException, ModelServiceException
	{
		return new ObjectUpdater ()
		{
			@Override
			public ObjectUpdater overwrite ( ModelObject withData )
			{
				fUpdates.add ( new Update ( objectPath, UpdateType.OVERWRITE, withData ) );
				return this;
			}

			@Override
			public ObjectUpdater merge ( ModelObject withData )
			{
				fUpdates.add ( new Update ( objectPath, UpdateType.MERGE, withData ) );
				return this;
			}

			@Override
			public ObjectUpdater replaceAcl ( AccessControlList acl )
			{
				fUpdates.add ( new Update ( objectPath, acl ) );
				return this;
			}

			@Override
			public void execute () throws ModelRequestException, ModelSchemaViolationException, ModelServiceException
			{
				for ( Update mu : fUpdates )
				{
					mu.update ( context );
				}

				log.info ( "wrote {}", objectPath );
				
				// we no longer have an accurate view of the remote object
				context.remove ( objectPath );
			}

			private final LinkedList<Update> fUpdates = new LinkedList<> ();
		};
	}

	private enum UpdateType
	{
		OVERWRITE,
		MERGE,
		ACL
	}
	private class Update
	{
		public Update ( Path path, UpdateType ut, ModelObject data )
		{
			fPath = path;
			fType = ut;
			fData = data;
//			fAcl = null;
		}

		public void update ( ModelRequestContext context ) throws ModelRequestException, ModelServiceException
		{
			if ( fType == UpdateType.ACL )
			{
				throw new ModelRequestException ( "This model does not currently support ACL updates." );
			}

			HttpRequest req = fClient.newRequest ()
				.asUser ( fCreds )
				.onPath ( pathToUrl ( fPath ) )
			;

			try ( 
				final HttpResponse resp = fType == UpdateType.OVERWRITE ?
					req.put ( JsonModelObject.modelObjectToJson ( fData ) ) :
					req.patch ( JsonModelObject.modelObjectToJson ( fData ) )
			)
			{
				if ( resp.isClientError () )
				{
					throw new ModelRequestException ( "server replied " + resp.getCode () + " " + resp.getMessage () );
				}
				else if ( resp.isServerError () )
				{
					throw new ModelServiceException ( "server replied " + resp.getCode () + " " + resp.getMessage () );
				}
			}
			catch ( HttpServiceException e )
			{
				throw new ModelServiceException ( e );
			}
		}

		public Update ( Path path, AccessControlList acl )
		{
			// FIXME: at some point we'll support an ACL update.

			fPath = path;
			fType = UpdateType.ACL;
			fData = null;
//			fAcl = acl;
		}

		private final Path fPath;
		private final UpdateType fType;
		private final ModelObject fData;
//		private final AccessControlList fAcl;
	}

	@Override
	public boolean remove ( ModelRequestContext context, Path objectPath ) throws ModelServiceException, ModelRequestException
	{
		// regardless of what happens at the server, we'll drop the object from our cache
		context.remove ( objectPath );

		// signal delete to server
		final String path = pathToUrl ( objectPath );
		try ( 
			final HttpResponse resp = fClient.newRequest ()
				.asUser ( fCreds )
				.onPath ( path )
				.delete ( )
			)
		{
			if ( resp.isSuccess () )
			{
				final JSONObject respBody = resp.getBody ();
				return respBody.optBoolean ( "removal", false );
			}
			else if ( resp.isClientError () )
			{
				throw new ModelRequestException ( "server replied " + resp.getCode () + " " + resp.getMessage () );
			}
			else
			{
				throw new ModelServiceException ( "server replied " + resp.getCode () + " " + resp.getMessage () );
			}
		}
		catch ( HttpServiceException | BodyFormatException e )
		{
			throw new ModelServiceException ( e );
		}
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
		return new LocalRelationSelector ( this, objectPath );
	}

	private final String fAcctId;
	private final String fModelId;
	private final JsonOverHttpClient fClient;
	private final HttpUsernamePasswordCredentials fCreds;
	private final String fBaseUrl;
	private final Path fPathPrefix;

	private static final Logger log = LoggerFactory.getLogger ( CommonJsonDbModel.class );

	private String getBasePath ( String section )
	{
		return fBaseUrl + "/v1/" + section;
	}

	private String encodeString ( String text )
	{
		return TypeConvertor.urlEncode ( text );
	}

	private String encodePath ( Path p )
	{
		return encodeString ( fPathPrefix.makeChildPath ( p ).toString ().substring ( 1 ) );
	}

	private String pathToUrl ( final Path objectPath )
	{
		return getBasePath("model") + "/" + encodePath ( objectPath );
	}

	private List<ModelRelationInstance> getRelns ( Path forObject, boolean inbound, String relnName ) throws ModelItemDoesNotExistException, ModelRequestException, ModelServiceException
	{
		final LinkedList<ModelRelationInstance> result = new LinkedList<> ();

		final String path = getBasePath ("relations") + (inbound?"/in/":"/out/") + encodePath ( forObject );  
		try (
			final HttpResponse resp = fClient.newRequest ()
				.asUser ( fCreds )
				.onPath ( path )
				.get ()
			)
		{
			if ( resp.isSuccess () )
			{
				final JSONObject respBody = resp.getBody ();
				
				final JSONObject relns = respBody.getJSONObject ( "relations" );
				final JSONObject dir = relns.getJSONObject ( inbound ? "in" : "out" );
				JsonVisitor.forEachElement ( dir, new ObjectVisitor<JSONArray,JSONException> ()
				{
					@Override
					public boolean visit ( String relnName, JSONArray srcObjPath ) throws JSONException
					{
						JsonVisitor.forEachElement ( srcObjPath, new ArrayVisitor<String,JSONException> ()
						{
							@Override
							public boolean visit ( String srcObj ) throws JSONException
							{
								// FIXME: get ID from service
								final Path srcPath = Path.fromString ( srcObj );
								result.add ( ModelRelationInstance.from ( ModelRelation.from ( inbound ? srcPath : forObject, relnName, inbound ? forObject : srcPath ) ) );
								return true;
							}
							
						} );
						return true;
					}
				} );

				return result;
			}
			else if ( resp.isNotFound () )
			{
				throw new ModelItemDoesNotExistException ( forObject );
			}
			else if ( resp.isClientError () )
			{
				throw new ModelRequestException ( "server replied " + resp.getCode () + " " + resp.getMessage () );
			}
			else
			{
				throw new ModelServiceException ( "server replied " + resp.getCode () + " " + resp.getMessage () );
			}
		}
		catch ( HttpServiceException | BodyFormatException e )
		{
			throw new ModelServiceException ( e );
		}
	}

	private class RemoteModelQuery extends SimpleModelQuery
	{
		@Override
		public <T,K> ModelObjectList<T> execute ( ModelRequestContext context, ModelObjectFactory<T,K> factory, DataAccessor<T> accessor, K userContext ) throws ModelRequestException, ModelServiceException
		{
			final LinkedList<ModelObjectAndPath<T>> result = new LinkedList<> ();

			final ModelPathList objectPaths = listChildrenOfPath ( context, getPathPrefix () );
			for ( Path objectPath : objectPaths )
			{
				final T mo = load ( context, objectPath, factory, userContext );
				boolean match = true;
				for ( Filter f : getFilters() )
				{
					match = f.matches ( accessor.getDataFrom ( mo ) );
					if ( !match )
					{
						break;
					}
				}
				if ( match )
				{
					result.add ( ModelObjectAndPath.from ( objectPath, mo ) );
				}
			}

			// now sort our list
			Comparator<ModelObject> orderBy = getOrdering ();
			if ( orderBy != null )
			{
				Collections.sort ( result, new Comparator<ModelObjectAndPath<T>> ()
				{
					@Override
					public int compare ( ModelObjectAndPath<T> o1, ModelObjectAndPath<T> o2 )
					{
						return orderBy.compare (
							accessor.getDataFrom ( o1.getObject () ),
							accessor.getDataFrom ( o2.getObject () )
						);
					}
				} );
			}

			return new ModelObjectList<T> ()
			{
				@Override
				public Iterator<ModelObjectAndPath<T>> iterator ()
				{
					return result.iterator ();
				}
			};
		}
	}

	@Override
	protected void onStopRequested ()
	{
		fClient.close ();
	}
}
