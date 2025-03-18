/*
 *  Copyright (c) 2006-2025 Continual.io. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.continual.http.app.servers.endpoints;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.app.servers.CorsOptionsRouter;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.iam.IamService;
import io.continual.iam.access.AccessDb;
import io.continual.iam.access.Resource;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.UserContext;
import io.continual.services.ServiceContainer;
import io.continual.util.standards.HttpStatusCodes;

public class TypicalRestApiEndpoint<I extends Identity> extends JsonIoEndpoint
{
	public static final String kSetting_ContinualProductTag = "apiKeyProductTag";
	public static final String kContinualProductTag = "continual";
	public static final String kContinualSystemsGroup = "continualSystems";

	public static final String kSetting_AuthLineHeader = "http.auth.header";
	public static final String kSetting_DateLineHeader = "http.date.header";
	public static final String kSetting_MagicLineHeader = "http.magic.header";

	public static final String kDefault_AuthLineHeader = "X-Continual-Auth";
	public static final String kDefault_DateLineHeader = "X-Continual-Date";
	public static final String kDefault_MagicLineHeader = "X-Continual-Magic";

	public interface Authenticator<I extends Identity>
	{
		I authenticate ( IamService<I,?> am, CHttpRequestContext context ) throws IamSvcException;
	}

	/**
	 * An API handler that's provided the context and an authenticated user.
	 *
	 * @param <I>
	 */
	public interface ApiHandler<I extends Identity>
	{
		/**
		 * Handle the request as the given user
		 * 
		 * @param context the request context
		 * @param uc the user context
		 * @throws IOException 
		 */
		void handle ( CHttpRequestContext context, UserContext<I> uc ) throws IOException;
	}

	/**
	 * Make a simple resource reference from the given name
	 * @param named
	 * @return a resource
	 */
	public static Resource makeResource ( String named ) { return Resource.fromName ( named ); } 

	/**
	 * A resource access statement used to express a resource and operation that a user must be allowed.
	 */
	public static class ResourceAccess
	{
		public ResourceAccess ( String resourceId, String operation )
		{
			fResource = Resource.fromName ( resourceId );
			fOp = operation;
		}

		public final Resource fResource;
		public final String fOp;
	}

	/**
	 * Construct the base endpoint with a service container and specific settings
	 * @param sc
	 * @param settings
	 * @throws BuildFailure
	 */
	@SuppressWarnings("unchecked")
	public TypicalRestApiEndpoint ( ServiceContainer sc, JSONObject settings ) throws BuildFailure
	{
		fAccts = sc.getReqd ( getAcctsSvcName ( settings ), IamService.class );
		fAuthenticator = new AuthList<I> ( settings );
	}

	/**
	 * Handle the given request with the given ApiHandler after authentication
	 * @param context
	 * @param handler
	 */
	public void handleWithApiAuth ( CHttpRequestContext context, ApiHandler<I> handler )
	{
		handleWithApiAuthAndAccess ( context, handler );
	}

	/**
	 * Handle the given request with the given ApiHandler after authentication and access control checks.
	 * @param context
	 * @param handler
	 * @param accessReqd
	 */
	public void handleWithApiAuthAndAccess ( CHttpRequestContext context, ApiHandler<I> handler, ResourceAccess... accessReqd )
	{
		try
		{
			// add cors headers
			CorsOptionsRouter.setupCorsHeaders ( context );

			// get the user
			final UserContext<I> user = getUser ( context );
			if ( user == null )
			{
				sendNotAuth ( context );
				return;
			}

			// check for required access
			final String uid = user.getEffectiveUserId ();
			final AccessDb<?> adb = fAccts.getAccessDb ();
			for ( ResourceAccess ra : accessReqd )
			{
				if ( !adb.canUser ( uid, ra.fResource, ra.fOp ) )
				{
					senForbidden ( context );
					return;
				}
			}

			// access allowed, call the handler
			handler.handle ( context, user );
		}
		catch ( IOException e )
		{
			log.warn ( e.getMessage () );
			sendStatusCodeAndMessage ( context, HttpStatusCodes.k500_internalServerError, "I/O problem writing the response, but... you got it???" );
		}
		catch ( JSONException | IamSvcException e )
		{
			log.warn ( e.getMessage (), e );
			sendStatusCodeAndMessage ( context, HttpStatusCodes.k500_internalServerError, "There was a problem handling your API request." );
		}
	}

	/**
	 * Can the given user perform the given operation on the given resource?
	 * @param context
	 * @param user
	 * @param resId
	 * @param operation
	 * @return true if the user is permitted
	 */
	public boolean canUser ( CHttpRequestContext context, UserContext<Identity> user, String resId, String operation ) throws IamSvcException
	{
		final boolean result = fAccts.getAccessDb ().canUser ( user.getEffectiveUserId (), makeResource ( resId ), operation );
		if ( !result )
		{
			log.info ( user.toString () + " cannot " + operation + " object " + resId );
		}
		return result;
	}

	/**
	 * Get the current user via authentication and return a user context
	 * @param context
	 * @return a UserContext or null of the user is not authenticated
	 * @throws IamSvcException
	 */
	public UserContext<I> getUser ( final CHttpRequestContext context ) throws IamSvcException
	{
		final IamService<I,?> am = fAccts;

		UserContext<I> result = null;
		I authUser = null;
		try
		{
			// get this user authenticated
			authUser = fAuthenticator.authenticate ( am, context );

			// if we have an authentic user, build a user context
			if ( authUser != null )
			{
				final String authFor = context.request ().getFirstHeader ( "X-AuthFor" );
				if ( authFor != null && authFor.length () > 0 && !authFor.equals ( authUser.getId () ) )
				{
					// authorized user is vouching for another...
					
					// get that user's identity
					final I authForUser = am.getIdentityDb ().loadUser ( authFor );

					// if the user exists and this user is authorized...
					if ( authForUser != null && authUser.getGroup ( kContinualSystemsGroup ) != null )
					{
						// auth user is part of the special systems group
						result = new UserContext.Builder<I> ()
							.forUser ( authForUser )
							.sponsoredByUser ( authUser )
							.build ()
						;
					}
					// else: this is a bogus request
				}
				else	// no auth-for or it's the same user
				{
					result = new UserContext.Builder<I> ()
						.forUser ( authUser )
						.build ()
					;
				}
			}

			return result;
		}
		catch ( IamSvcException x )
		{
			log.warn ( "Error processing authentication: " + x.getMessage () );
			throw x;
		}
	}

	protected IamService<I,?> getInternalAccts ()
	{
		return fAccts;
	}

	private final IamService<I, ?> fAccts;
	private final Authenticator<I> fAuthenticator;

	private static final Logger log = LoggerFactory.getLogger ( TypicalRestApiEndpoint.class );

	// configs have used different conventions before the lookup was centralized here
	public static String getAcctsSvcName ( JSONObject settings )
	{
		for ( String key : kAcctSvcKeys )
		{
			final String acctSvcName = settings.optString ( key, null );
			if ( acctSvcName != null ) 
			{
				return acctSvcName;
			}
		}
		return kAcctSvcKeys[0];
	}
	private static String[] kAcctSvcKeys = new String[] { "accounts", "accountsService" };
}
