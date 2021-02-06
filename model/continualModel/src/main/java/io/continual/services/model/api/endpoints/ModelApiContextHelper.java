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

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.http.util.http.standards.MimeTypes;
import io.continual.iam.IamServiceManager;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.UserContext;
import io.continual.restHttp.ApiContextHelper;
import io.continual.restHttp.HttpServlet;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelStdUserGroups;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.service.ModelService;
import io.continual.util.naming.Path;

public class ModelApiContextHelper extends ApiContextHelper
{
	public ModelApiContextHelper ( ModelService ms )
	{
		fModelService = ms;
	}

	public interface ModelApiContext
	{
		CHttpRequestContext getHttpContext ();

		ModelRequestContext getModelRequestContext ();

		HttpServlet getHttpServlet ();

		UserContext getUserContext ();

		IamServiceManager<?,?> getAccountService ();

		ModelService getModelService ();

		Path getRequestedPath ();
	}

	public interface ModelApiHandler
	{
		/**
		 * Handle the request as the given user and return a JSON string.
		 * 
		 * @param modelApiContext
		 * @return a JSON string
		 * @throws JSONException 
		 * @throws IOException 
		 * @throws ModelServiceRequestException 
		 */
		String handle ( ModelApiContext modelApiContext ) throws IOException, JSONException, ModelServiceRequestException;
	}

	/**
	 * Authenticate the user and setup base paths, etc. for model service use.
	 * @param context
	 * @param handler
	 * @throws ModelServiceRequestException
	 */
	public void handleModelRequest ( final CHttpRequestContext context, final String acctId, final String path, final ModelApiHandler handler ) throws ModelServiceRequestException
	{
		try
		{
			final HttpServlet servlet = (HttpServlet) context.getServlet ();
			final IamServiceManager<?,?> as = getAccountsSvc ( context );

			setupCorsHeaders ( context );

			final UserContext userContext = getUser ( as, context );
			if ( userContext == null )
			{
				sendNotAuth ( context );
				return;
			}
			
			// the user can specify another account. if null, use his own
//			String baseAcctId = acctId;
//			if ( acctId == null )
//			{
//				baseAcctId = userContext.getEffectiveUserId ();
//			}

			String pathToUse = path;
			if ( path != null && !path.startsWith ( "/" ) )
			{
				pathToUse = "/" + path;
			}
			final Path actualPath = path == null ? null : Path.fromString ( pathToUse );

			// setup a request context
			final ModelRequestContext mrc = fModelService.buildRequestContext ()
				.forUser ( userContext.getUser () )
				.build ()
			;
			
			final ModelApiContext mac = new ModelApiContext ()
			{
				@Override
				public ModelRequestContext getModelRequestContext () { return mrc; }
				
				@Override
				public CHttpRequestContext getHttpContext () { return context; }

				@Override
				public HttpServlet getHttpServlet () { return servlet; }

				@Override
				public UserContext getUserContext () { return userContext; }

				@Override
				public IamServiceManager<?, ?> getAccountService () { return as; }

				@Override
				public ModelService getModelService () { return fModelService; }

				@Override
				public Path getRequestedPath () { return actualPath; }
			};
		
			final String response = handler.handle ( mac );
			if ( response != null )
			{
				context.response().getStreamForTextResponse ( MimeTypes.kAppJson ).println ( response );
			}
		}
		catch ( ModelServiceIoException | IamSvcException e )
		{
			log.warn ( e.getMessage () );
			sendJson ( context, HttpStatusCodes.k503_serviceUnavailable, new JSONObject ().put ( "status", HttpStatusCodes.k503_serviceUnavailable ).put ( "message", "There was a problem handling your request." ) );
		}
		catch ( IOException e )
		{
			log.warn ( e.getMessage () );
			context.response ().sendError ( HttpStatusCodes.k500_internalServerError, "I/O problem writing the response, but... you got it???" );
		}
		catch ( JSONException e )
		{
			log.warn ( e.getMessage () );
			context.response ().sendError ( HttpStatusCodes.k500_internalServerError, "There was a problem handling your API request." );
		}
	}

	public void handleModelAdminRequest ( final CHttpRequestContext context, final String acctId, final String path, final ModelApiHandler handler ) throws ModelServiceRequestException
	{
		try
		{
			final IamServiceManager<?,?> as = getAccountsSvc ( context );
			final UserContext user = getUser ( as, context );
			if ( user == null )
			{
				sendNotAuth ( context );
				return;
			}

			final Group sysAdmins = as.getAccessDb ().loadGroup ( ModelStdUserGroups.kSysAdminGroup );
			if ( !sysAdmins.isMember ( user.getActualUserId () ) )
			{
				sendNotAuth ( context );
				return;
			}
		}
		catch ( IamSvcException e )
		{
			log.warn ( e.getMessage () );
			context.response ().sendError ( HttpStatusCodes.k500_internalServerError, "There was a problem handling your API request." );
		}

		handleModelRequest ( context, acctId, path, handler );
	}

//	private static final String kUserData_HomeModel = "";
//	private static final String kHomeDirPrefix = "~/";

//	public static Path getEffectivePath ( Identity user, String requestedPath ) throws ModelServiceRequestException
//	{
//		if ( requestedPath == null )
//		{
//			return null;
//		}
//
//		if ( requestedPath.startsWith ( kHomeDirPrefix ) )
//		{
//			try
//			{
//				// use the user's "home" model. This can be set on the user record. If not,
//				// use the standard location
//				String homeModel = user.getUserData ( kUserData_HomeModel );
//				if ( homeModel == null )
//				{
//					homeModel = "/accounts/" + user.getId ();
//				}
//				requestedPath = homeModel + "/" + requestedPath.substring ( kHomeDirPrefix.length () );
//			}
//			catch ( IamSvcException e )
//			{
//				throw new ModelServiceRequestException ( e );
//			}
//		}
//
//		if ( !requestedPath.startsWith ( "/" ) )
//		{
//			requestedPath = "/" + requestedPath;
//		}
//
//		return Path.fromString ( requestedPath );
//	}

	private ModelService fModelService;

	private static final Logger log = LoggerFactory.getLogger ( ModelApiContextHelper.class );
}
