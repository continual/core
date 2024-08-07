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
import java.util.TreeSet;

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
import io.continual.services.model.core.ModelPathListPage;
import io.continual.services.model.core.ModelQuery;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelTraversal;
import io.continual.services.model.core.PageRequest;
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
	public ModelClient ( String modelId, String baseUrl, Path pathPrefix, String username, String password )
	{
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
	public ModelPathListPage listChildrenOfPath ( ModelRequestContext context, Path prefix, PageRequest pr ) throws ModelServiceException, ModelRequestException
	{
		try ( 
			final HttpResponse resp = fClient.newRequest ()
				.asUser ( fCreds )
				.onPath ( pathToUrl ( prefix ) )
				.addQueryParam ( "children", "1" )
				.addQueryParam ( "pg", pr.getRequestedPage () )
				.addQueryParam ( "sz", pr.getRequestedPageSize () )
				.get ()
			)
		{
			final LinkedList<Path> result = new LinkedList<> ();
			
			JsonVisitor.forEachElement ( resp.getBody ().optJSONArray ( "children" ), new ArrayVisitor<String,JSONException> () 
			{
				@Override
				public boolean visit ( String path ) throws JSONException
				{
					result.add ( modelPathToUserPath ( Path.fromString ( path ) ) );
					return true;
				}
			} );

			// FIXME: this paging data won't work - if original request is page 2 with 25 items, this would show page 0 with 25 total
	        return ModelPathListPage.wrap ( result, pr );
		}
		catch ( HttpServiceException | JSONException | BodyFormatException e )
		{
			throw new ModelServiceException ( e );
		}
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
	public ModelRelationInstance relate ( ModelRequestContext context, ModelRelation userReln ) throws ModelServiceException, ModelRequestException
	{
		// rebuild the relation using the base path
		final ModelRelation reln = ModelRelation.from (
			userPathToModelPath ( userReln.getFrom () ),
			userReln.getName (),
			userPathToModelPath ( userReln.getTo () )
		);

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
			private boolean fOverwrite = false;
			private JSONObject fData = null;
			private AccessControlList fAcl = null;
			private TreeSet<String> fAddTypes = new TreeSet<> ();
			private TreeSet<String> fRemTypes = new TreeSet<> ();

			@Override
			public ObjectUpdater overwriteData ( ModelObject withData )
			{
				fData = JsonModelObject.modelObjectToJson ( withData );
				fOverwrite = true;
				return this;
			}

			@Override
			public ObjectUpdater mergeData ( ModelObject withData )
			{
				fData = JsonModelObject.modelObjectToJson ( withData );
				fOverwrite = false;
				return this;
			}

			@Override
			public ObjectUpdater replaceAcl ( AccessControlList acl )
			{
				fAcl = acl;
				return this;
			}

			@Override
			public ObjectUpdater addTypeLock ( String typeId )
			{
				fAddTypes.add ( typeId );
				return this;
			}

			@Override
			public ObjectUpdater removeTypeLock ( String typeId )
			{
				fRemTypes.add ( typeId );
				return this;
			}

			@Override
			public void execute () throws ModelRequestException, ModelSchemaViolationException, ModelServiceException
			{
				// build a payload and send it
				HttpRequest req = fClient.newRequest ()
					.asUser ( fCreds )
					.onPath ( pathToUrl ( objectPath ) )
				;

				// make typing updates
				for ( String typeId : fAddTypes )
				{
					req.withHeader ( "X-ContinualModel-LockType", typeId );
				}
				for ( String typeId : fRemTypes )
				{
					req.withHeader ( "X-ContinualModel-UnlockType", typeId );
				}
				if ( fAcl != null )
				{
					req.withHeader ( "X-ContinualModel-AccessControlList", fAcl.serialize () );
				}

				// send the request
				try ( 
					final HttpResponse resp = fOverwrite ?
						req.put ( fData ) :
						req.patch ( fData )
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
				finally
				{
					context.remove ( objectPath );
				}

				log.info ( "wrote {}", objectPath );
			}
		};
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

	private Path userPathToModelPath ( Path p )
	{
		return fPathPrefix.makeChildPath ( p );
	}

	private Path modelPathToUserPath ( Path p )
	{
		return p.makePathWithinParent ( fPathPrefix );
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

			final ModelPathListPage objectPaths = listChildrenOfPath ( context, getPathPrefix () );
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
