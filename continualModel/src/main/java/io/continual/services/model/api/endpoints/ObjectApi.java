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

package io.continual.services.model.api.endpoints;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.iam.access.AccessControlList;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.services.model.core.ModelObjectUpdater;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.service.Model;
import io.continual.services.model.service.ModelRelation;
import io.continual.services.model.service.ModelService;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.naming.Path;


/**
 * The date API provides the current servers date/time
 */
public class ObjectApi extends ModelApiContextHelper
{
	public ObjectApi ( ModelService ms )
	{
		super ( ms );
	}

	public static Path makeObjectPath ( String path )
	{
		if ( path == null ) return null;

		if ( !path.startsWith ( "/" ) ) path = "/" + path;
		return Path.fromString ( path );
	}

	public void getObject ( CHttpRequestContext context, final String acctId, final String modelName, final String objectPath ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				try
				{
					final ModelRequestContext mrc = modelApiContext.getModelRequestContext ();
					final Model model = modelApiContext.getModelService().getAccount ( mrc, acctId ).getModel ( mrc, modelName );
					final Path path = makeObjectPath ( objectPath );

					return new ObjectRenderer ( path )
						.withData ( model.load ( mrc, path ) )
						.withRelations ( model.getRelations ( mrc, makeObjectPath ( objectPath ) ) )
						.renderText ()
					;
				}
				catch ( IllegalArgumentException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "couldn't read response type" );
					return null;
				}
			}
		} );
	}

	public void getObjectData ( CHttpRequestContext context, final String acctId, final String modelName, final String objectPath ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				try
				{
					final ModelRequestContext mrc = modelApiContext.getModelRequestContext ();
					final Model model = modelApiContext.getModelService().getAccount ( mrc, acctId ).getModel ( mrc, modelName );
					final Path path = makeObjectPath ( objectPath );

					return new ObjectRenderer ( path )
						.withData ( model.load ( mrc, path ) )
						.renderText ()
					;
				}
				catch ( IllegalArgumentException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "couldn't read response type" );
					return null;
				}
			}
		} );
	}

	public void getObjectRelations ( CHttpRequestContext context, final String acctId, final String modelName, final String objectPath ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				try
				{
					final ModelRequestContext mrc = modelApiContext.getModelRequestContext ();
					final Model model = modelApiContext.getModelService().getAccount ( mrc, acctId ).getModel ( mrc, modelName );
					final Path path = makeObjectPath ( objectPath );

					return new ObjectRenderer ( path )
						.withRelations ( model.getRelations ( mrc, path ) )
						.renderText ()
					;
				}
				catch ( IllegalArgumentException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "couldn't read response type" );
					return null;
				}
			}
		} );
	}

	public void getInboundObjectRelations ( CHttpRequestContext context, final String acctId, final String modelName, final String objectPath ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				try
				{
					final ModelRequestContext mrc = modelApiContext.getModelRequestContext ();
					final Model model = modelApiContext.getModelService().getAccount ( mrc, acctId ).getModel ( mrc, modelName );
					final Path path = makeObjectPath ( objectPath );

					return new ObjectRenderer ( path )
						.withInboundRelnsOnly ()
						.withRelations ( model.getInboundRelations ( mrc, path ) )
						.renderText ()
					;
				}
				catch ( IllegalArgumentException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "couldn't read response type" );
					return null;
				}
			}
		} );
	}

	public void getInboundObjectRelationsNamed ( CHttpRequestContext context, final String acctId, final String modelName, final String objectPath, final String named ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				try
				{
					final ModelRequestContext mrc = modelApiContext.getModelRequestContext ();
					final Model model = modelApiContext.getModelService().getAccount ( mrc, acctId ).getModel ( mrc, modelName );
					final Path path = makeObjectPath ( objectPath );

					return new ObjectRenderer ( path )
						.withInboundRelnsOnly ()
						.withRelnName ( named )
						.withRelations ( model.getInboundRelationsNamed ( mrc, path, named ) )
						.renderText ()
					;
				}
				catch ( IllegalArgumentException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "couldn't read response type" );
					return null;
				}
			}
		} );
	}

	public void getOutboundObjectRelations ( CHttpRequestContext context, final String acctId, final String modelName, final String objectPath ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				try
				{
					final ModelRequestContext mrc = modelApiContext.getModelRequestContext ();
					final Model model = modelApiContext.getModelService().getAccount ( mrc, acctId ).getModel ( mrc, modelName );
					final Path path = makeObjectPath ( objectPath );

					return new ObjectRenderer ( path )
						.withOutboundRelnsOnly ()
						.withRelations ( model.getOutboundRelations ( mrc, path ) )
						.renderText ()
					;
				}
				catch ( IllegalArgumentException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "couldn't read response type" );
					return null;
				}
			}
		} );
	}

	public void getOutboundObjectRelationsNamed ( CHttpRequestContext context, final String acctId, final String modelName, final String objectPath, final String named ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				try
				{
					final ModelRequestContext mrc = modelApiContext.getModelRequestContext ();
					final Model model = modelApiContext.getModelService().getAccount ( mrc, acctId ).getModel ( mrc, modelName );
					final Path path = makeObjectPath ( objectPath );

					return new ObjectRenderer ( path )
						.withOutboundRelnsOnly ()
						.withRelnName ( named )
						.withRelations ( model.getOutboundRelationsNamed ( mrc, path, named ) )
						.renderText ()
					;
				}
				catch ( IllegalArgumentException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "couldn't read response type" );
					return null;
				}
			}
		} );
	}

	public void putObject ( CHttpRequestContext context, final String acctId, final String modelName, final String objectPath ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				JSONObject obj;
				try
				{
					obj = new JSONObject ( new CommentedJsonTokener ( context.request ().getBodyStream () ) );
				}
				catch ( JSONException e )
				{
					// parsing failure
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "couldn't parse your JSON" );
					return null;
				}

				// store the object
				final ModelRequestContext mrc = modelApiContext.getModelRequestContext ();
				modelApiContext
					.getModelService ()
					.getAccount ( mrc, acctId )
					.getModel ( mrc, modelName )
					.store ( mrc, makeObjectPath (objectPath), obj.toString () );

				sendStatusCodeAndMessage ( context, HttpStatusCodes.k202_accepted, "accepted, no content" );
				return null;
			}
		} );
	}

	public void patchObject ( CHttpRequestContext context, final String acctId, final String modelName, final String objectPath ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				final JSONObject patchSource;
				try
				{
					patchSource = new JSONObject ( new CommentedJsonTokener ( context.request ().getBodyStream () ) );
				}
				catch ( JSONException e )
				{
					// parsing failure
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "couldn't parse your JSON" );
					return null;
				}

				// run the update
				final ModelRequestContext mrc = modelApiContext.getModelRequestContext ();
				modelApiContext
					.getModelService ()
					.getAccount ( mrc, acctId )
					.getModel ( mrc, modelName )
					.update ( mrc, makeObjectPath ( objectPath ),
						new ModelObjectUpdater ()
						{
							@Override
							public ModelObject update ( ModelObject patchTarget )
							{
								final JSONObject oo = new JSONObject ( new CommentedJsonTokener ( patchTarget.asJson () ) );
								JsonUtil.copyInto ( patchSource, oo );
								return new ModelObject ()
								{
									@Override
									public AccessControlList getAccessControlList ()
									{
										// TODO Auto-generated method stub
										// FIXME
										return null;
									}

									@Override
									public String getId ()
									{
										// TODO Auto-generated method stub
										// FIXME
										return null;
									}
									@Override
									public String asJson ()
									{
										return oo.toString ();
									}

									@Override
									public Set<String> getTypes ()
									{
										return new TreeSet<> ();
									}

									@Override
									public JSONObject getData ()
									{
										// TODO Auto-generated method stub
										return null;
									}
								};
							}
						}
					)
				;

				sendStatusCodeAndMessage ( context, HttpStatusCodes.k204_noContent, "patched, no content" );
				return null;
			}
		} );
	}

	public void deleteObject ( CHttpRequestContext context, final String acctId, final String modelName, final String objectPath ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				// remove the object
				final ModelRequestContext mrc = modelApiContext.getModelRequestContext ();
				modelApiContext
					.getModelService ()
					.getAccount ( mrc, acctId )
					.getModel ( mrc, modelName )
					.remove ( mrc, makeObjectPath ( objectPath ) )
				;

				sendStatusCodeAndMessage ( context, HttpStatusCodes.k204_noContent, "removed" );
				return null;
			}
		} );
	}

	public void postRelation ( CHttpRequestContext context, final String acctId, final String modelName ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				final LinkedList<ModelRelation> relns = new LinkedList<> ();
				try
				{
					final JSONObject topLevel = JsonUtil.readJsonObject ( context.request ().getBodyStream () );
					JsonVisitor.forEachElement ( topLevel.optJSONArray ( "relations" ), new ArrayVisitor<JSONObject,JSONException> () {
						@Override
						public boolean visit ( JSONObject t ) throws JSONException
						{
							relns.add ( makeModelRelation ( acctId, modelName,
								t.getString ( "from" ),
								t.getString ( "name" ),
								t.getString ( "to" )
							) );
	
							return true;
						}
					} );
				}
				catch ( JSONException x )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "couldn't parse inbound json" );
					return null;
				}

				final ModelRequestContext mrc = modelApiContext.getModelRequestContext ();
				modelApiContext
					.getModelService ()
					.getAccount ( mrc, acctId )
					.getModel ( mrc, modelName )
					.relate ( mrc, relns )
				;

				sendStatusCodeAndMessage ( context, HttpStatusCodes.k201_created, "relations created" );
				return null;
			}
		} );
	}

	public void putRelation ( CHttpRequestContext context, final String acctId, final String modelName, final String fromName, final String relnName, final String toName ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				final ModelRequestContext mrc = modelApiContext.getModelRequestContext ();
				modelApiContext
					.getModelService ()
					.getAccount ( mrc, acctId )
					.getModel ( mrc, modelName )
					.relate ( mrc, makeModelRelation ( acctId, modelName, fromName, relnName, toName ) )
				;

				sendStatusCodeAndMessage ( context, HttpStatusCodes.k201_created, "relations created" );
				return null;
			}
		} );
	}

	public void deleteRelation ( CHttpRequestContext context, final String acctId, final String modelName, final String fromName, final String relnName, final String toName ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				final ModelRequestContext mrc = modelApiContext.getModelRequestContext ();
				modelApiContext
					.getModelService ()
					.getAccount ( mrc, acctId )
					.getModel ( mrc, modelName )
					.unrelate ( mrc, makeModelRelation ( acctId, modelName, fromName, relnName, toName ) )
				;

				sendStatusCodeAndMessage ( context, HttpStatusCodes.k204_noContent, "removal complete" );
				return null;
			}
		} );

		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k501_notImplemented, "not implemented" );
				return null;
			}
		} );
	}

	private static ModelRelation makeModelRelation ( String acctId, String modelName, String fromName, String relnName, String toName )
	{
		return new ModelRelation ()
		{
			@Override
			public ModelObjectPath getTo () { return new ModelObjectPath ( acctId, modelName, makeObjectPath ( toName ) ); }

			@Override
			public String getName () { return relnName; }

			@Override
			public ModelObjectPath getFrom () { return new ModelObjectPath ( acctId, modelName, makeObjectPath ( fromName ) ); }
		};
	}
}
