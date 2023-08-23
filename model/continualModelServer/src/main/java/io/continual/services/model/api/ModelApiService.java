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

package io.continual.services.model.api;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.CHttpErrorHandler;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.restHttp.HttpService;
import io.continual.restHttp.HttpServlet;
import io.continual.restHttp.TypicalHttpRouter;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.services.model.api.endpoints.AuthApiHandler;
import io.continual.services.model.api.endpoints.MetricsApiHandler;
import io.continual.services.model.api.endpoints.ModelApi;
import io.continual.services.model.api.endpoints.Options;
import io.continual.services.model.core.exceptions.ModelAccessException;
import io.continual.services.model.core.exceptions.ModelItemDoesNotExistException;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.service.ModelService;
import io.continual.util.nv.NvReadable;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.standards.MimeTypes;

public class ModelApiService extends SimpleService
{
	private static final String kSetting_HttpServiceName = "httpService";
	private static final String kDefault_HttpServiceName = "http";

	private static final String kSetting_ModelServiceName = "modelService";
	private static final String kDefault_ModelServiceName = "model";

	public ModelApiService ( ServiceContainer sc, JSONObject settings ) throws BuildFailure 
	{
		final HttpService server = sc.get ( settings.optString ( kSetting_HttpServiceName, kDefault_HttpServiceName ), HttpService.class );
		if ( server == null ) throw new BuildFailure ( "An HTTP service is required." );

		final ModelService ms = sc.get ( settings.optString ( kSetting_ModelServiceName, kDefault_ModelServiceName ), ModelService.class );
		if ( ms == null ) throw new BuildFailure ( "A model service is required." );

		final ModelApi modelApi = new ModelApi ( ms );
		final AuthApiHandler authApi = new AuthApiHandler ( sc, settings );

		server.addRouter ( "modelApi", new TypicalHttpRouter ()
		{
			public void setupRouter ( HttpServlet servlet, CHttpRequestRouter rr, NvReadable p ) throws IOException, BuildFailure
			{
				rr.setHandlerForException ( CHttpRequestRouter.noMatchingRoute.class,
					new CHttpErrorHandler ()
					{
						@Override
						public void handle ( CHttpRequestContext ctx, Throwable cause )
						{
							ctx.response ().sendErrorAndBody ( HttpStatusCodes.k404_notFound, 
								new JSONObject ()
									.put ( "error", HttpStatusCodes.k404_notFound )
									.put ( "message", "Not found. See the API docs." )
									.toString (),
								MimeTypes.kAppJson );
						}
					} );

				rr.setHandlerForException ( ModelAccessException.class,
					new CHttpErrorHandler ()
					{
						@Override
						public void handle ( CHttpRequestContext ctx, Throwable cause )
						{
							ctx.response ().sendErrorAndBody ( HttpStatusCodes.k403_forbidden, 
								new JSONObject ()
									.put ( "error", HttpStatusCodes.k403_forbidden )
									.put ( "message", "Unauthorized: " + cause.getMessage () )
									.toString (),
								MimeTypes.kAppJson );
						}
					} );

				rr.setHandlerForException ( ModelItemDoesNotExistException.class,
					new CHttpErrorHandler ()
					{
						@Override
						public void handle ( CHttpRequestContext ctx, Throwable cause )
						{
							ctx.response ().sendErrorAndBody ( HttpStatusCodes.k404_notFound, 
								new JSONObject ()
									.put ( "error", HttpStatusCodes.k404_notFound )
									.put ( "message", "Not found: " + cause.getMessage () )
									.toString (),
								MimeTypes.kAppJson );
						}
					} );

				rr.setHandlerForException ( ModelRequestException.class,
					new CHttpErrorHandler ()
					{
						@Override
						public void handle ( CHttpRequestContext ctx, Throwable cause )
						{
							ctx.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest, 
								new JSONObject ()
									.put ( "error", HttpStatusCodes.k400_badRequest )
									.put ( "message", cause.getMessage () )
									.toString (),
								MimeTypes.kAppJson );
						}
					} );

				rr.setHandlerForException ( JSONException.class,
					new CHttpErrorHandler ()
					{
						@Override
						public void handle ( CHttpRequestContext ctx, Throwable cause )
						{
							ctx.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest, 
								new JSONObject ()
									.put ( "error", HttpStatusCodes.k400_badRequest )
									.put ( "message", "Bad request. See the API docs." )
									.toString (),
								MimeTypes.kAppJson );
						}
					} );

				rr.setHandlerForException ( Throwable.class,
					new CHttpErrorHandler ()
					{
						@Override
						public void handle ( CHttpRequestContext ctx, Throwable cause )
						{
							ctx.response ().sendErrorAndBody ( HttpStatusCodes.k500_internalServerError, 
								new JSONObject ()
									.put ( "error", HttpStatusCodes.k500_internalServerError )
									.put ( "message", "There was a problem at the server." )
									.toString (),
								MimeTypes.kAppJson );
							log.warn ( cause.getMessage (), cause );
						}
					} );

				loadConfigs ( rr, new ConfigAndClass[] {
					new ConfigAndClass<AuthApiHandler> ( "authRoutes.conf", authApi ),
					new ConfigAndClass<ModelApi> ( "modelApi.conf", modelApi ),
					new ConfigAndClass<MetricsApiHandler> ( "metrics.conf", null ),
					new ConfigAndClass<Options> ( "options.conf", null ),
				});
			}
		} );
	}

	private static final Logger log = LoggerFactory.getLogger ( ModelApiService.class );
}
