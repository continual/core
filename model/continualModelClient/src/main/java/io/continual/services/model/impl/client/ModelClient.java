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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.jsonHttpClient.HttpUsernamePasswordCredentials;
import io.continual.jsonHttpClient.JsonOverHttpClient;
import io.continual.jsonHttpClient.JsonOverHttpClient.BodyFormatException;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpResponse;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpServiceException;
import io.continual.jsonHttpClient.impl.ok.OkHttp;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectComparator;
import io.continual.services.model.core.ModelObjectList;
import io.continual.services.model.core.ModelPathList;
import io.continual.services.model.core.ModelQuery;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.SimpleModelQuery;
import io.continual.services.model.impl.json.CommonJsonDbModel;
import io.continual.services.model.impl.json.CommonJsonDbObject;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;
import io.continual.util.naming.Path;

public class ModelClient extends CommonJsonDbModel
{
	public ModelClient ( String acctId, String modelId, String baseUrl, Path pathPrefix, String username, String password )
	{
		super ( acctId, modelId );

		fClient = new OkHttp ();
		fBaseUrl = baseUrl;
		fPathPrefix = pathPrefix;
		fCreds = new HttpUsernamePasswordCredentials ( username, password );
	}

	public ModelClient ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config );
		try
		{
			final ExpressionEvaluator ee = sc.getExprEval ();

			fClient = new OkHttp ();
			fBaseUrl = config.getString ( "baseUrl" );
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

	private class RemoteModelQuery extends SimpleModelQuery
	{
		@Override
		public ModelObjectList execute ( ModelRequestContext context ) throws ModelRequestException, ModelServiceException
		{
			final LinkedList<ModelObject> result = new LinkedList<> ();

			final ModelPathList objectPaths = listChildrenOfPath ( context, getPathPrefix () );
			for ( Path objectPath : objectPaths )
			{
				final ModelObject mo = load ( context, objectPath );
				boolean match = true;
				for ( Filter f : getFilters() )
				{
					match = f.matches ( mo );
					if ( !match )
					{
						break;
					}
				}
				if ( match )
				{
					result.add ( mo );
				}
			}

			// now sort our list
			ModelObjectComparator orderBy = getOrdering ();
			if ( orderBy != null )
			{
				Collections.sort ( result, new java.util.Comparator<ModelObject> ()
				{
					@Override
					public int compare ( ModelObject o1, ModelObject o2 )
					{
						return orderBy.compare ( o1, o2 );
					}
				} );
			}

			return new ModelObjectList ()
			{
				@Override
				public Iterator<ModelObject> iterator ()
				{
					return result.iterator ();
				}
			};
		}
	}
	
	@Override
	public ModelQuery startQuery ()
	{
		return new RemoteModelQuery ();
	}

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

	@Override
	protected ModelObject loadObject ( ModelRequestContext context, final Path objectPath ) throws ModelServiceException, ModelItemDoesNotExistException, ModelRequestException
	{
		// localhost:8080/v1/model/home%2foo

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
				return new CommonJsonDbObject ( objectPath.toString (), obj );
			}
			else if ( resp.isNotFound () )
			{
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

	@Override
	protected void internalStore ( ModelRequestContext context, Path objectPath, ModelObject o ) throws ModelRequestException, ModelServiceException
	{
		final String path = pathToUrl ( objectPath );
		try ( 
			final HttpResponse resp = fClient.newRequest ()
				.asUser ( fCreds )
				.onPath ( path )
				.put ( o.getData () )
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

	@Override
	protected boolean internalRemove ( ModelRequestContext context, Path objectPath ) throws ModelRequestException, ModelServiceException
	{
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
	public ModelRelationInstance relate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		final String path = getBasePath ("relations") + "/" + encodePath ( reln.getFrom () ) + "/" + encodeString ( reln.getName () ) + "/" + encodePath ( reln.getTo () );  
		try ( 
			final HttpResponse resp = fClient.newRequest ()
				.asUser ( fCreds )
				.onPath ( path )
				.put ( new JSONObject () )
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

			// FIXME: return ID from service
			return ModelRelationInstance.from ( reln );
		}
		catch ( HttpServiceException e )
		{
			throw new ModelServiceException ( e );
		}
	}

	@Override
	public boolean unrelate ( ModelRequestContext context, ModelRelation reln ) throws ModelServiceException, ModelRequestException
	{
		final String path = getBasePath ("relations") + "/" + encodePath ( reln.getFrom () ) + "/" + encodeString ( reln.getName () ) + "/" + encodePath ( reln.getTo () );  
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
	public boolean unrelate ( ModelRequestContext context, String relnId ) throws ModelServiceException, ModelRequestException
	{
		// FIXME: this needs to use id from service
		try
		{
			final ModelRelationInstance mr = ModelRelationInstance.from ( relnId );
			return unrelate ( context, mr );
		}
		catch ( IllegalArgumentException x )
		{
			throw new ModelRequestException ( x );
		}
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
	
	@Override
	public List<ModelRelationInstance> getInboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		return getRelns ( forObject, true, named );
	}

	@Override
	public List<ModelRelationInstance> getOutboundRelationsNamed ( ModelRequestContext context, Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		return getRelns ( forObject, false, named );
	}

	private final JsonOverHttpClient fClient;
	private final HttpUsernamePasswordCredentials fCreds;
	private final String fBaseUrl;
	private final Path fPathPrefix;
}
