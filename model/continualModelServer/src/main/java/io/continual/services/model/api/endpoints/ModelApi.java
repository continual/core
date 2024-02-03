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
import io.continual.iam.exceptions.IamSvcException;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.core.updaters.DataMerge;
import io.continual.services.model.core.updaters.DataOverwrite;
import io.continual.services.model.service.ModelService;
import io.continual.services.model.session.ModelSession;
import io.continual.util.collections.MultiMap;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ItemRenderer;
import io.continual.util.naming.Path;
import io.continual.util.standards.HttpStatusCodes;

public class ModelApi extends ModelApiContextHelper
{
	public ModelApi ( ModelService ms )
	{
		super ( ms );
	}

	public void getModelListForAccount ( CHttpRequestContext context ) throws IOException, ModelRequestException
	{
		handleModelRequest ( context, null, null, new ModelApiHandler ()
		{
			@Override
			public void handle ( ModelApiContext modelApiContext ) throws IOException, JSONException
			{
				modelApiContext.respondOk ( new JSONObject ()
					.put ( "status", HttpStatusCodes.k200_ok )
					.put ( "and...", "not really implemented in transition" )
				);
//					.put ( "models", JsonVisitor.listToArray ( modelApiContext.getModelSession ().getModelMounts () ) ) )
			}
		} );
	}

	public void getModelIndexes ( CHttpRequestContext context, final String acctId, final String modelName ) throws IOException, ModelRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public void handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelRequestException
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k501_notImplemented, "not implemented" );
			}
		} );
	}

	public void getModelIndex ( CHttpRequestContext context, final String acctId, final String modelName, final String indexName ) throws IOException, ModelRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public void handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelRequestException
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k501_notImplemented, "not implemented" );
			}
		} );
	}

	public void createModelIndex ( CHttpRequestContext context, final String acctId, final String modelName, final String indexName ) throws IOException, ModelRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public void handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelRequestException
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k501_notImplemented, "not implemented" );
			}
		} );
	}

	public void dropModelIndex ( CHttpRequestContext context, final String acctId, final String modelName, final String indexName ) throws IOException, ModelRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public void handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelRequestException
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k501_notImplemented, "not implemented" );
			}
		} );
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

	public static final String kIncludeParam = "incl";

	public static enum IncludeOptions
	{
		DATA,
		RELS,
		BOTH
	};

	public static final String kIncludeParam_Default = IncludeOptions.BOTH.toString ();

	private static IncludeOptions userTextToOption ( String text ) throws ModelRequestException
	{
		try
		{
			return IncludeOptions.valueOf ( text.toUpperCase () );
		}
		catch ( IllegalArgumentException e )
		{
			throw new ModelRequestException ( "Unrecognized option for " + kIncludeParam + ": " + text );
		}
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

				final IncludeOptions io = userTextToOption ( 
					context.request().getParameter ( kIncludeParam, kIncludeParam_Default )
				);

				final Model model = ms.getModel ();
				if ( model.exists ( mrc, requestedPath ) )
				{
					final JSONObject response = new JSONObject ()
						.put ( "status", HttpStatusCodes.k200_ok )
						.put ( "request", requestedPath.toString () )
					;

					final ObjectRenderer or = new ObjectRenderer ()
						.atPath ( requestedPath )
					;

					if ( io == IncludeOptions.DATA || io == IncludeOptions.BOTH )
					{
						final ModelObject mo = ms.getModel ().load ( mrc, requestedPath );
						or
							.withData ( mo )
						;
					}

					if ( io == IncludeOptions.RELS || io == IncludeOptions.BOTH )
					{
						or.withRelations ( ms.getModel ().getRelations ( mrc, requestedPath ) );
					}

					response.put ( "object", or.render () );

					modelApiContext.respondOk ( response );
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

				final List<ModelRelationInstance> relns = ms.getModel().getOutboundRelationsNamed ( mrc, requestedPath, context.request ().getParameter ( "rn", null ) );

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

				final List<ModelRelationInstance> relns = ms.getModel().getInboundRelationsNamed ( mrc, requestedPath, context.request ().getParameter ( "rn", null ) );

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

				final List<ModelRelationInstance> inRelns = ms.getModel().getInboundRelations ( mrc, requestedPath );
				final List<ModelRelationInstance> outRelns = ms.getModel().getOutboundRelations ( mrc, requestedPath );
				
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

	private static JSONObject renderRelnSet ( List<ModelRelationInstance> relns, boolean fromSide )
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

//	public void getModel ( CHttpRequestContext context, final String acctId, final String modelName ) throws IOException, ModelServiceRequestException
//	{
//		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
//		{
//			@Override
//			public String handle ( ModelApiContext modelApiContext )
//				throws IOException, JSONException, ModelServiceRequestException
//			{
//				final ModelService ms = modelApiContext.getModelService ();
//				final ModelAccount ma = ms.getAccount ( modelApiContext.getModelRequestContext (), acctId );
//				final Model model = ma.getModel ( modelApiContext.getModelRequestContext (), modelName );
//
//				if ( model == null )
//				{
//					sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "the model does not exist" );
//					return null;
//				}
//				
//				// return the result
//				return new JSONObject ()
//					.put ( "status", HttpStatusCodes.k200_ok )
//					.put ( "model", model.toJson() )
//					.toString ()
//				;
//			}
//		} );
//	}

//	public void createModel ( CHttpRequestContext context, final String acctId, final String modelName ) throws IOException, ModelServiceRequestException
//	{
//		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
//		{
//			@Override
//			public String handle ( ModelApiContext modelApiContext )
//				throws IOException, JSONException, ModelServiceRequestException
//			{
//				// load the account path
//				final ModelRequestContext mrc = modelApiContext.getModelRequestContext ();
//				final ModelService ms = modelApiContext.getModelService ();
//				final ModelAccount ma = ms.getAccount ( mrc, acctId );
//				if ( ma.doesModelExist ( mrc, modelName ) )
//				{
//					sendStatusCodeAndMessage ( context, HttpStatusCodes.k409_conflict, "Model [" + modelName + "] exists. You must explicitly delete it before creating it again." );
//					return null;
//				}
//
//				final Model model = ma.initModel ( modelApiContext.getModelRequestContext (), modelName );
//				
//				// return the result
//				return new JSONObject ()
//					.put ( "status", HttpStatusCodes.k200_ok )
//					.put ( "model", model.toJson () )
//					.toString ()
//				;
//			}
//		} );
//	}
//
//	public void deleteModel ( CHttpRequestContext context, final String acctId, final String modelName ) throws IOException, ModelServiceRequestException
//	{
//		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
//		{
//			@Override
//			public String handle ( ModelApiContext modelApiContext )
//				throws IOException, JSONException, ModelServiceRequestException
//			{
//				sendStatusCodeAndMessage ( context, HttpStatusCodes.k501_notImplemented, "not implemented" );
//				return null;
//			}
//		} );
//	}
//
//	public void selectWithParam ( CHttpRequestContext context, final String acctId, final String modelName ) throws IOException, ModelServiceRequestException
//	{
//		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
//		{
//			@Override
//			public String handle ( ModelApiContext modelApiContext )
//				throws IOException, JSONException, ModelServiceRequestException
//			{
//				sendStatusCodeAndMessage ( context, HttpStatusCodes.k501_notImplemented, "not implemented" );
//				return null;
//			}
//		} );
//	}
//
//	public void selectWithBody ( CHttpRequestContext context, final String acctId, final String modelName ) throws IOException, ModelServiceRequestException
//	{
//		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
//		{
//			@Override
//			public String handle ( ModelApiContext modelApiContext )
//				throws IOException, JSONException, ModelServiceRequestException
//			{
//				sendStatusCodeAndMessage ( context, HttpStatusCodes.k501_notImplemented, "not implemented" );
//				return null;
//			}
//		} );
//	}

	/*
	 * 				// request the child elements of the account. these are the models from an API perspective.
				final JSONArray a = new JSONArray ();
				for ( Path m : o.getElementsBelow ( modelApiContext.getModelRequestContext () ).getElements () )
				{
					a.put ( m.getItemName () );
				}


	 */
/*
	public static void getModel ( CHttpRequestContext context, String modelName ) throws IOException
	{
		getModel ( context, null, modelName );
	}

	public static void getModel ( CHttpRequestContext context, final String acctId, final String modelName ) throws IOException
	{
		handleWithApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, Identity user )
			{
				try
				{
					final Account effectiveAcct = getEffectiveAccount ( acctId, context, acct, user );
					final OtterleyRequestContext ctx = new OtterleyRequestContext ( user );
					final OtterleyModel m = effectiveAcct.getModel ( modelName, ctx );
					if ( m == null )
					{
						throw new ModelKeyspaceException ( "Model [" + modelName + "] does not exist." );
					}
					return modelToJson ( modelName, m ).toString ();
				}
				catch ( JSONException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k500_internalServerError,
						"There was an error writing the response body.", MimeTypes.kPlainText );
					log.error ( "Couldn't writ emodel info. " + e.getMessage() );
				}
				catch ( ModelKeyspaceException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k404_notFound, "Entity does not exist.", MimeTypes.kPlainText );
				}
				catch ( OtterleySecurityException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k401_unauthorized, "Not authorized.", MimeTypes.kPlainText );
				}
				catch ( ModelIoException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k500_internalServerError, "Couldn't load model: " + e.getMessage(), MimeTypes.kPlainText );
				}
				catch ( CHttpAccountsException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k404_notFound, "Entity does not exist.", MimeTypes.kPlainText );
				}
				return null;
			}
		} );
	}

	public static void createModel ( CHttpRequestContext context, final String modelName ) throws IOException
	{
		createModel ( context, null, modelName );
	}

	public static void createModel ( CHttpRequestContext context, final String acctId, final String modelName ) throws IOException
	{
		handleWithApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, Identity user )
			{
				try
				{
					final Account effectiveAcct = getEffectiveAccount ( acctId, context, acct, user );

					// read multiple in case there are 0
					final List<JSONObject> modelSpec = JsonBodyReader.readBodyForObjects ( context );

					if ( modelSpec.size() == 0 || modelSpec.size() > 1 )
					{
						throw new OdbBadProvisioningRequestException ( "This request must have exactly 1 model specification." );
					}

					// does the model exist?
					if ( effectiveAcct.modelExists ( modelName ) )
					{
						context.response ().sendErrorAndBody ( HttpStatusCodes.k405_methodNotAllowed,
							"This model exists; please explicitly delete it first.", MimeTypes.kPlainText );
						return null;
					}
					else
					{
						final nvWriteableTable settings = new nvWriteableTable ();
						
						final OtterleyRequestContext ctx = new OtterleyRequestContext ( user );
						ApiContextHelper.getModelSvc(context).createModel (
							ApiContextHelper.getAccountsSvc ( context ),
							effectiveAcct, modelName, settings, ctx, user );

						final OtterleyModel newModel = effectiveAcct.getModel ( modelName, ctx );
						return modelToJson ( modelName, newModel ).toString ();
					}
				}
				catch ( JSONException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest,
						"There was an error provisioning the request: " + e.getMessage(), MimeTypes.kPlainText );
					log.error ( "Provisioning request refused: " + e.getMessage() );
				}
				catch ( IOException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k503_serviceUnavailable,
						"There was an error reading the request.", MimeTypes.kPlainText );
					log.error ( "Couldn't write model info. " + e.getMessage() );
				}
				catch ( OdbBadProvisioningRequestException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest,
						"There was an error provisioning the request: " + e.getMessage(), MimeTypes.kPlainText );
					log.error ( "Provisioning request refused: " + e.getMessage() );
				}
				catch ( ModelKeyspaceException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k404_notFound, "Entity does not exist.", MimeTypes.kPlainText );
				}
				catch ( OtterleySecurityException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k401_unauthorized, "Not authorized.", MimeTypes.kPlainText );
				}
				catch ( ModelIoException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k500_internalServerError, "Couldn't load model: " + e.getMessage(), MimeTypes.kPlainText );
				}
				catch ( CHttpAccountsException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k404_notFound, "Entity does not exist.", MimeTypes.kPlainText );
				}
				return null;
			}
		} );
	}

	public static void deleteModel ( CHttpRequestContext context, final String modelName ) throws IOException
	{
		deleteModel ( context, null, modelName );
	}

	public static void deleteModel ( CHttpRequestContext context, final String acctId, final String modelName ) throws IOException
	{
		handleWithApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, Identity user )
			{
				try
				{
					final OtterleyRequestContext ctx = new OtterleyRequestContext ( user );
					final Account effectiveAcct = getEffectiveAccount ( acctId, context, acct, user );
					ApiContextHelper.getModelSvc ( context ).deleteModel ( ApiContextHelper.getAccountsSvc ( context ),
						effectiveAcct, modelName, ctx, user );
				}
				catch ( ModelIoException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k500_internalServerError, "Couldn't remove model.", MimeTypes.kPlainText );
				}
				catch ( OdbBadProvisioningRequestException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k500_internalServerError, "Couldn't remove model.", MimeTypes.kPlainText );
				}
				catch ( CHttpAccountsException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k404_notFound, "Entity does not exist.", MimeTypes.kPlainText );
				}
				catch ( OtterleySecurityException e )
				{
					context.response().sendError ( HttpStatusCodes.k401_unauthorized, "not authorized" );
				}
				return null;
			}
		} );
	}

	public static void getTypes ( CHttpRequestContext context, final String modelName ) throws IOException
	{
		getTypes ( context, null, modelName );
	}

	public static void getTypes ( CHttpRequestContext context, final String acctId, final String modelName ) throws IOException
	{
		handleWithApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, Identity user ) throws IOException
			{
				try
				{
					final OtterleyRequestContext ctx = new OtterleyRequestContext ( user );
					final OtterleyModel m = acct.getModel ( modelName, ctx );
					if ( m == null )
					{
						throw new ModelKeyspaceException ( "Model [" + modelName + "] does not exist." );
					}

					final Collection<String> types = m.getAllTypeNames ( ctx );

					final PrintWriter pw = context.response ().getStreamForTextResponse ( MimeTypes.kAppJson );
					pw.print ( "{\"types\"=[" );

					boolean doneAny = false;
					for ( String typeName : types )
					{
						if ( doneAny ) pw.print ( "," );
						doneAny = true;

						pw.print ( "\"" );
						pw.print ( typeName );	//FIXME: escape quotes in the string
						pw.print ( "\"" );
					}

					pw.println ( "]}" );
					pw.close ();

					return null;
				}
				catch ( ModelKeyspaceException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k404_notFound, "Entity does not exist.", MimeTypes.kPlainText );
				}
				catch ( OtterleySecurityException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k401_unauthorized, "Not authorized.", MimeTypes.kPlainText );
				}
				catch ( ModelIoException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k500_internalServerError, "Couldn't load model.", MimeTypes.kPlainText );
				}
				return null;
			}
		} );
	}

	public static void getType ( CHttpRequestContext context, final String modelName, final String typeName ) throws IOException
	{
		getType ( context, null, modelName, typeName );
	}

	public static void getType ( CHttpRequestContext context, final String acctId, final String modelName, final String typeName ) throws IOException
	{
		handleWithApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, Identity user ) throws IOException
			{
				try
				{
					final Account effectiveAcct = getEffectiveAccount ( acctId, context, acct, user );

					final OtterleyRequestContext ctx = new OtterleyRequestContext ( user );
					final OtterleyModel m = effectiveAcct.getModel ( modelName, ctx );
					if ( m == null )
					{
						throw new ModelKeyspaceException ( "Model [" + modelName + "] does not exist." );
					}

					final OtterleyType t = m.loadType ( typeName, ctx );
					final JSONObject j = OtterleyJsonTypeSerializer.write ( t );
					return j.toString ();
				}
				catch ( OtterleySecurityException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k401_unauthorized,
						"Could not read object.", MimeTypes.kPlainText );
				}
				catch ( ModelKeyspaceException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest,
						"Entity does not exist.", MimeTypes.kPlainText );
				}
				catch ( ModelIoException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k503_serviceUnavailable,
						"Error writing the object.", MimeTypes.kPlainText );
				}
				catch ( CHttpAccountsException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k401_unauthorized,
						"Could not read object.", MimeTypes.kPlainText );
				}
				catch ( SerializerException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k500_internalServerError,
						"Couldn't serialize the type object.", MimeTypes.kPlainText );
				}
				return null;
			}
		});
	}

	public static void putType ( CHttpRequestContext context, final String modelName, final String typeName ) throws IOException
	{
		putType ( context, null, modelName, typeName );
	}
	
	public static void putType ( CHttpRequestContext context, final String acctId, final String modelName, final String typeName ) throws IOException
	{
		handleWithApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, Identity user ) throws IOException
			{
				try
				{
					final JSONObject in = JsonBodyReader.readBody ( context );

					final Account effectiveAcct = getEffectiveAccount ( acctId, context, acct, user );

					final OtterleyRequestContext ctx = new OtterleyRequestContext ( user );
					final OtterleyModel m = effectiveAcct.getModel ( modelName, ctx );
					if ( m == null )
					{
						throw new ModelKeyspaceException ( "Model [" + modelName + "] does not exist." );
					}

					final OtterleyTypeUpdater ii = new OtterleyTypeUpdater ()
					{
						public void update ( OtterleyType o ) throws ModelIoException
						{
							OtterleyJsonTypeSerializer.read ( in, o );
							o.put ( OtterleyType.kSetting_Name, typeName );
						}
					};
					m.replaceType ( typeName, ii, ctx );

					context.response ().setStatus ( HttpStatusCodes.k204_noContent );
				}
				catch ( IOException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest,
						"There was a problem reading your input.", MimeTypes.kPlainText );
				}
				catch ( JSONException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest,
						"Couldn't parse your JSON object.", MimeTypes.kPlainText );
				}
				catch ( OtterleySecurityException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k401_unauthorized,
						"Could not read object.", MimeTypes.kPlainText );
				}
				catch ( ModelKeyspaceException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest,
						"Entity does not exist.", MimeTypes.kPlainText );
				}
				catch ( ModelIoException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k503_serviceUnavailable,
						"Error writing the object.", MimeTypes.kPlainText );
				}
				catch ( OtterleyTypeException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest,
						"The object had a type exception. " + e.getMessage (), MimeTypes.kPlainText );
				}
				catch ( CHttpAccountsException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k401_unauthorized,
						"Could not read object.", MimeTypes.kPlainText );
				}
				return null;
			}
		});
	}

	public static void deleteType ( CHttpRequestContext context, final String modelName, final String typeName ) throws IOException
	{
		deleteType ( context, null, modelName, typeName );
	}

	public static void deleteType ( CHttpRequestContext context, final String acctId, final String modelName, final String typeName ) throws IOException
	{
		handleWithApiAuth ( context, new ApiHandler ()
		{
			@Override
			public String handle ( CHttpRequestContext context, HttpServlet servlet, Identity user ) throws IOException
			{
				try
				{
					final Account effectiveAcct = getEffectiveAccount ( acctId, context, acct, user );

					final OtterleyRequestContext ctx = new OtterleyRequestContext ( user );
					final OtterleyModel m = effectiveAcct.getModel ( modelName, ctx );
					if ( m == null )
					{
						throw new ModelKeyspaceException ( "Model [" + modelName + "] does not exist." );
					}

					m.deleteType ( typeName, ctx );
					context.response ().setStatus ( HttpStatusCodes.k204_noContent );
				}
				catch ( OtterleySecurityException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k401_unauthorized,
						"Could not read object.", MimeTypes.kPlainText );
				}
				catch ( ModelKeyspaceException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest,
						"Entity does not exist.", MimeTypes.kPlainText );
				}
				catch ( ModelIoException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k503_serviceUnavailable,
						"Error writing the object.", MimeTypes.kPlainText );
				}
				catch ( CHttpAccountsException e )
				{
					context.response ().sendErrorAndBody ( HttpStatusCodes.k401_unauthorized,
						"Could not read object.", MimeTypes.kPlainText );
				}
				return null;
			}
		});
	}

//	public static void getObjectListeners ( CHttpRequestContext context, final String modelName, final String objectId ) throws IOException
//	{
//		handleWithApiAuth ( context, new apiHandler ()
//		{
//			@Override
//			public String handle ( CHttpRequestContext context, nebbyDbServlet servlet, nebbyAccount acct, nebbyUser user )
//			{
//				try
//				{
//					final nebbyRequestContext ctx = new nebbyRequestContext ( user );
//					final nebbyModel m = acct.getModel ( modelName, ctx );
//					if ( m == null )
//					{
//						throw new nebbyKeyspaceException ( "Model [" + modelName + "] does not exist." );
//					}
//					final nebbyObject o = m.load ( objectId, ctx );
//
//					return "[]";
//				}
//				catch ( nebbySecurityException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k401_unauthorized,
//						"Could not read object.", MimeTypes.kPlainText );
//				}
//				catch ( nebbyKeyspaceException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k404_notFound, "Entity does not exist.", MimeTypes.kPlainText );
//				}
//				catch ( nebbyIoException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k500_internalServerError,
//						"There was an error reading from the model.", MimeTypes.kPlainText );
//					log.severe ( "Couldn't read from model: " + e.getMessage() );
//				}
//				return null;
//			}
//		} );
//	}
//
//	public static void getObjectListener ( CHttpRequestContext context, final String modelName, final String oid, final String typeName ) throws IOException
//	{
//		handleWithApiAuth ( context, new apiHandler ()
//		{
//			@Override
//			public String handle ( CHttpRequestContext context, nebbyDbServlet servlet, nebbyAccount acct, nebbyUser user ) throws IOException
//			{
//				try
//				{
//					final nebbyRequestContext ctx = new nebbyRequestContext ( user );
//					final nebbyModel m = acct.getModel ( modelName, ctx );
//					if ( m == null )
//					{
//						throw new nebbyKeyspaceException ( "Model [" + modelName + "] does not exist." );
//					}
//
//					final nebbyType t = m.loadType ( typeName, ctx );
//					final JSONObject j = jsonType.write ( t );
//					return j.toString ();
//				}
//				catch ( nebbySecurityException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k401_unauthorized,
//						"Could not read object.", MimeTypes.kPlainText );
//				}
//				catch ( nebbyKeyspaceException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest,
//						"Entity does not exist.", MimeTypes.kPlainText );
//				}
//				catch ( nebbyIoException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k503_serviceUnavailable,
//						"Error writing the object.", MimeTypes.kPlainText );
//				}
//				return null;
//			}
//		});
//	}
//
//	public static void addObjectListener ( CHttpRequestContext context, final String modelName, final String oid, final String typeName ) throws IOException
//	{
//		handleWithApiAuth ( context, new apiHandler ()
//		{
//			@Override
//			public String handle ( CHttpRequestContext context, nebbyDbServlet servlet, nebbyAccount acct, nebbyUser user ) throws IOException
//			{
//				try
//				{
//					final JSONObject in = JsonBodyReader.readBody ( context );
//
//					final nebbyRequestContext ctx = new nebbyRequestContext ( user );
//					final nebbyModel m = acct.getModel ( modelName, ctx );
//					if ( m == null )
//					{
//						throw new nebbyKeyspaceException ( "Model [" + modelName + "] does not exist." );
//					}
//
//					final nebbyModel.typeReader ii = new nebbyModel.typeReader ()
//					{
//						public void update ( nebbyType o ) throws nebbyIoException
//						{
//							jsonType.read ( in, o );
//							o.put ( nebbyType.kSetting_Name, typeName );
//						}
//					};
//					m.replaceType ( typeName, ii, ctx );
//
//					context.response ().setStatus ( HttpStatusCodes.k204_noContent );
//				}
//				catch ( IOException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest,
//						"There was a problem reading your input.", MimeTypes.kPlainText );
//				}
//				catch ( JSONException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest,
//						"Couldn't parse your JSON object.", MimeTypes.kPlainText );
//				}
//				catch ( nebbySecurityException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k401_unauthorized,
//						"Could not read object.", MimeTypes.kPlainText );
//				}
//				catch ( nebbyKeyspaceException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest,
//						"Entity does not exist.", MimeTypes.kPlainText );
//				}
//				catch ( nebbyIoException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k503_serviceUnavailable,
//						"Error writing the object.", MimeTypes.kPlainText );
//				}
//				catch ( nebbyTypeException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest,
//						"The object had a type exception. " + e.getMessage (), MimeTypes.kPlainText );
//				}
//				return null;
//			}
//		});
//	}
//
//	public static void removeObjectListener ( CHttpRequestContext context, final String modelName, final String oid, final String typeName ) throws IOException
//	{
//		handleWithApiAuth ( context, new apiHandler ()
//		{
//			@Override
//			public String handle ( CHttpRequestContext context, nebbyDbServlet servlet, nebbyAccount acct, nebbyUser user ) throws IOException
//			{
//				try
//				{
//					final nebbyRequestContext ctx = new nebbyRequestContext ( user );
//					final nebbyModel m = acct.getModel ( modelName, ctx );
//					if ( m == null )
//					{
//						throw new nebbyKeyspaceException ( "Model [" + modelName + "] does not exist." );
//					}
//
//					m.deleteType ( typeName, ctx );
//					context.response ().setStatus ( HttpStatusCodes.k204_noContent );
//				}
//				catch ( nebbySecurityException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k401_unauthorized,
//						"Could not read object.", MimeTypes.kPlainText );
//				}
//				catch ( nebbyKeyspaceException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest,
//						"Entity does not exist.", MimeTypes.kPlainText );
//				}
//				catch ( nebbyIoException e )
//				{
//					context.response ().sendErrorAndBody ( HttpStatusCodes.k503_serviceUnavailable,
//						"Error writing the object.", MimeTypes.kPlainText );
//				}
//				return null;
//			}
//		});
//	}

	protected static JSONObject modelToJson ( String name, OtterleyModel m ) throws JSONException
	{
		final JSONObject result = new JSONObject ();
		result.put ( "name", name );
//		result.put ( nebbyDbProvisioner.kSetting_DbAvailabilityType, m.getAvailabilityType() );
		return result;
	}
*/
}
