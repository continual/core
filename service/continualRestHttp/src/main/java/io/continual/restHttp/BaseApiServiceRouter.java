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

package io.continual.restHttp;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.CHttpErrorHandler;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.iam.access.AccessException;
import io.continual.util.nv.NvReadable;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.standards.MimeTypes;

public abstract class BaseApiServiceRouter implements HttpRouter
{
	protected void setupExceptionHandlers ( HttpServlet servlet, CHttpRequestRouter rr, NvReadable p ) throws IOException
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

		rr.setHandlerForException ( AccessException.class,
			new CHttpErrorHandler ()
			{
				@Override
				public void handle ( CHttpRequestContext ctx, Throwable cause )
				{
					ctx.response ().sendErrorAndBody ( HttpStatusCodes.k403_forbidden, 
						new JSONObject ()
							.put ( "error", HttpStatusCodes.k403_forbidden )
							.put ( "message", "Forbidden." )
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
	}

	private static final Logger log = LoggerFactory.getLogger ( BaseApiServiceRouter.class );
}
