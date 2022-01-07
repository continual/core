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
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.core.updaters.DataMerge;
import io.continual.services.model.core.updaters.DataOverwrite;
import io.continual.services.model.service.ModelService;
import io.continual.services.model.service.ModelSession;
import io.continual.util.collections.MultiMap;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ItemRenderer;
import io.continual.util.naming.Path;

public class ObjectApi extends ModelApiContextHelper
{
	public ObjectApi ( ModelService ms )
	{
		super ( ms );
	}

	// for completeness, we have a top-level request 
	public void getObject ( CHttpRequestContext context ) throws IOException, ModelRequestException
	{
		getObject ( context, "/" );
	}

	private ModelRequestContext makeMrc ( ModelApiContext modelApiContext, ModelSession ms ) throws BuildFailure
	{
		return ms.getModel().getRequestContextBuilder ()
			.forUser ( modelApiContext.getUserContext ().getUser () )
			.withSchemasFrom ( ms.getSchemaRegistry () )
			.withNotificationsTo ( ms.getNotificationSvc () )
			.build ()
		;
	}
	
	public void getObject ( CHttpRequestContext context, final String objectPath ) throws IOException, ModelRequestException
	{
		handleModelRequest ( context, null, objectPath, new ModelApiHandler ()
		{
			@Override
			public void handle ( ModelApiContext modelApiContext ) throws IOException, JSONException, IamSvcException, ModelItemDoesNotExistException, ModelRequestException, BuildFailure, ModelServiceException
			{
				final ModelSession ms = modelApiContext.getModelSession ();
				final Path requestedPath = modelApiContext.getRequestedPath ();
				final ModelRequestContext mrc = makeMrc ( modelApiContext, ms );

				final Model model = ms.getModel ();
				if ( model.exists ( mrc, requestedPath ) )
				{
					final ModelObject mo = ms.getModel ().load ( mrc, requestedPath );

					modelApiContext.respondOk ( new JSONObject ()
						.put ( "status", HttpStatusCodes.k200_ok )
						.put ( "request", requestedPath.toString () )
						.put ( "object", new ObjectRenderer ()
							.withData ( mo )
							.render ()
						)
					);
				}
				else
				{
					modelApiContext.respondWithStatus ( HttpStatusCodes.k404_notFound, new JSONObject ().put ( "path", objectPath.toString () ) );
				}
				
//				else
//				{
//					final JSONArray results = new JSONArray ();
//					final ModelPathList mpl = model.listObjectsStartingWith ( mrc, requestedPath );
//					if ( mpl == null )
//					{
//						modelApiContext.respondWithStatus ( HttpStatusCodes.k404_notFound,
//							new JSONObject ()
//								.put ( "status", HttpStatusCodes.k404_notFound )
//								.put ( "request", requestedPath.toString () )
//						);
//					}
//					else
//					{
//						for ( Path child : mpl )
//						{
//							results.put ( child.toString () );
//						}
//						modelApiContext.respondOk ( new JSONObject ()
//							.put ( "status", HttpStatusCodes.k200_ok )
//							.put ( "request", requestedPath.toString () )
//							.put ( "objects", results )
//						);
//					}
//				}
			}
		} );
	}

	public void putObject ( CHttpRequestContext context, final String objectPath ) throws IOException, ModelRequestException
	{
		handleModelRequest ( context, null, objectPath, new ModelApiHandler ()
		{
			@Override
			public void handle ( ModelApiContext modelApiContext ) throws IOException, JSONException, ModelServiceException, IamSvcException, ModelItemDoesNotExistException, ModelRequestException, BuildFailure
			{
				final JSONObject obj = readPayload ( context );

				final ModelSession ms = modelApiContext.getModelSession ();
				final Path requestedPath = modelApiContext.getRequestedPath ();
				final ModelRequestContext mrc = makeMrc ( modelApiContext, ms );

				ms.getModel ().store ( mrc, requestedPath, new DataOverwrite ( obj ) );
				modelApiContext.respondWithStatus ( HttpStatusCodes.k204_noContent, null );
			}
		} );
	}

	public void patchObject ( CHttpRequestContext context, final String objectPath ) throws IOException, ModelRequestException
	{
		handleModelRequest ( context, null, objectPath, new ModelApiHandler ()
		{
			@Override
			public void handle ( ModelApiContext modelApiContext ) throws IOException, JSONException, ModelServiceException, IamSvcException, ModelItemDoesNotExistException, ModelRequestException, BuildFailure
			{
				final JSONObject obj = readPayload ( context );

				final ModelSession ms = modelApiContext.getModelSession ();
				final Path requestedPath = modelApiContext.getRequestedPath ();
				final ModelRequestContext mrc = makeMrc ( modelApiContext, ms );

				ms.getModel ().store ( mrc, requestedPath, new DataMerge ( obj ) );
				modelApiContext.respondWithStatus ( HttpStatusCodes.k204_noContent, null );
			}
		} );
	}

	public void deleteObject ( CHttpRequestContext context, final String objectPath ) throws IOException, ModelRequestException
	{
		handleModelRequest ( context, null, objectPath, new ModelApiHandler ()
		{
			@Override
			public void handle ( ModelApiContext modelApiContext ) throws IOException, JSONException, ModelServiceException, IamSvcException, ModelItemDoesNotExistException, ModelRequestException, BuildFailure
			{
				final ModelSession ms = modelApiContext.getModelSession ();
				final Path requestedPath = modelApiContext.getRequestedPath ();
				final ModelRequestContext mrc = makeMrc ( modelApiContext, ms );

				final boolean removal = ms.getModel ().remove ( mrc, requestedPath );
				modelApiContext.respondWithStatus ( HttpStatusCodes.k200_ok, new JSONObject ().put ( "removal", removal ));
			}
		} );
	}

	public void putRelation ( CHttpRequestContext context, final String fromObjPath, final String relation, final String toObjPath ) throws IOException, ModelRequestException
	{
		handleModelRequest ( context, null, fromObjPath, new ModelApiHandler ()
		{
			@Override
			public void handle ( ModelApiContext modelApiContext ) throws IOException, JSONException, ModelServiceException, IamSvcException, ModelItemDoesNotExistException, ModelRequestException, BuildFailure
			{
				final Path toPath = fixupPath ( toObjPath );

				final ModelSession ms = modelApiContext.getModelSession ();
				final Path requestedPath = modelApiContext.getRequestedPath ();
				final ModelRequestContext mrc = makeMrc ( modelApiContext, ms );

				ms.getModel().relate ( mrc, new ModelRelation ()
				{
					@Override
					public Path getFrom () { return requestedPath; }

					@Override
					public Path getTo () { return toPath; }

					@Override
					public String getName () { return relation; }
				} );

				modelApiContext.respondWithStatus ( HttpStatusCodes.k204_noContent, null );
			}
		} );
	}

	public void deleteRelation ( CHttpRequestContext context, final String fromObjPath, final String relation, final String toObjPath ) throws IOException, ModelRequestException
	{
		handleModelRequest ( context, null, fromObjPath, new ModelApiHandler ()
		{
			@Override
			public void handle ( ModelApiContext modelApiContext ) throws IOException, JSONException, ModelServiceException, IamSvcException, ModelItemDoesNotExistException, ModelRequestException, BuildFailure
			{
				final Path toPath = fixupPath ( toObjPath );

				final ModelSession ms = modelApiContext.getModelSession ();
				final Path requestedPath = modelApiContext.getRequestedPath ();
				final ModelRequestContext mrc = makeMrc ( modelApiContext, ms );

				final boolean removal = ms.getModel().unrelate ( mrc, new ModelRelation ()
				{
					@Override
					public Path getFrom () { return requestedPath; }

					@Override
					public Path getTo () { return toPath; }

					@Override
					public String getName () { return relation; }
				} );

				modelApiContext.respondWithStatus ( HttpStatusCodes.k200_ok, new JSONObject ().put ( "removal", removal ));
			}
		} );
	}

	public void getOutboundRelations ( CHttpRequestContext context, final String objectPath ) throws IOException, ModelRequestException
	{
		handleModelRequest ( context, null, objectPath, new ModelApiHandler ()
		{
			@Override
			public void handle ( ModelApiContext modelApiContext ) throws IOException, JSONException, ModelServiceException, IamSvcException, ModelItemDoesNotExistException, ModelRequestException, BuildFailure
			{
				final ModelSession ms = modelApiContext.getModelSession ();
				final Path requestedPath = modelApiContext.getRequestedPath ();
				final ModelRequestContext mrc = makeMrc ( modelApiContext, ms );

				final List<ModelRelation> relns = ms.getModel().getOutboundRelationsNamed ( mrc, requestedPath, context.request ().getParameter ( "rn", null ) );

				modelApiContext.respondOk ( new JSONObject ()
					.put ( "status", HttpStatusCodes.k200_ok )
					.put ( "request", requestedPath.toString () )
					.put ( "relations", new JSONObject ()
						.put ( "out", renderRelnSet ( relns, false ) )
					)
				);
			}
		} );
	}

	public void getInboundRelations ( CHttpRequestContext context, final String objectPath ) throws IOException, ModelRequestException
	{
		handleModelRequest ( context, null, objectPath, new ModelApiHandler ()
		{
			@Override
			public void handle ( ModelApiContext modelApiContext ) throws IOException, JSONException, ModelServiceException, IamSvcException, ModelItemDoesNotExistException, ModelRequestException, BuildFailure
			{
				final ModelSession ms = modelApiContext.getModelSession ();
				final Path requestedPath = modelApiContext.getRequestedPath ();
				final ModelRequestContext mrc = makeMrc ( modelApiContext, ms );

				final List<ModelRelation> relns = ms.getModel().getInboundRelationsNamed ( mrc, requestedPath, context.request ().getParameter ( "rn", null ) );

				modelApiContext.respondOk ( new JSONObject ()
					.put ( "status", HttpStatusCodes.k200_ok )
					.put ( "request", requestedPath.toString () )
					.put ( "relations", new JSONObject ()
						.put ( "in", renderRelnSet ( relns, true ) )
					)
				);
			}
		} );
	}

	public void getAllRelations ( CHttpRequestContext context, final String objectPath ) throws IOException, ModelRequestException
	{
		handleModelRequest ( context, null, objectPath, new ModelApiHandler ()
		{
			@Override
			public void handle ( ModelApiContext modelApiContext ) throws IOException, JSONException, ModelServiceException, IamSvcException, ModelItemDoesNotExistException, ModelRequestException, BuildFailure
			{
				final ModelSession ms = modelApiContext.getModelSession ();
				final Path requestedPath = modelApiContext.getRequestedPath ();
				final ModelRequestContext mrc = makeMrc ( modelApiContext, ms );

				final List<ModelRelation> inRelns = ms.getModel().getInboundRelations ( mrc, requestedPath );
				final List<ModelRelation> outRelns = ms.getModel().getOutboundRelations ( mrc, requestedPath );
				
				modelApiContext.respondOk ( new JSONObject ()
					.put ( "status", HttpStatusCodes.k200_ok )
					.put ( "request", requestedPath.toString () )
					.put ( "relations", new JSONObject ()
						.put ( "in", renderRelnSet ( inRelns, true ) )
						.put ( "out", renderRelnSet ( outRelns, false ) )
					)
				);
			}
		} );
	}

	private static JSONObject readPayload ( CHttpRequestContext context ) throws IOException, ModelRequestException
	{
		try
		{
			return new JSONObject ( new CommentedJsonTokener ( context.request ().getBodyStream () ) );
		}
		catch ( JSONException e )
		{
			throw new ModelRequestException ( "Couldn't parse your JSON." );
		}
	}

	private static JSONObject renderRelnSet ( List<ModelRelation> relns, boolean fromSide )
	{
		final MultiMap<String,Path> relnMap = new MultiMap<> ();
		for ( ModelRelation mr : relns )
		{
			relnMap.put ( mr.getName (), fromSide ? mr.getFrom () : mr.getTo () );
		}

		final JSONObject obj = new JSONObject ();
		for ( String relnName : relnMap.getKeys () )
		{
			final List<Path> targets = relnMap.get ( relnName );
			obj.put ( relnName, JsonVisitor.listToArray ( targets, new ItemRenderer<Path,String> ()
			{
				@Override
				public String render ( Path p ) { return p.getId (); }
			} ) );
		}
		return obj;
	}
}
