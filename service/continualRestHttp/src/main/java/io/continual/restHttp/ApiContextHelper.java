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
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.context.CHttpResponse;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.http.util.http.standards.MimeTypes;
import io.continual.iam.IamAuthLog;
import io.continual.iam.IamServiceManager;
import io.continual.iam.access.Resource;
import io.continual.iam.credentials.ApiKeyCredential;
import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.credentials.JwtCredential.InvalidJwtToken;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.UserContext;
import io.continual.iam.impl.common.ApiKeyAuthHelper;
import io.continual.iam.impl.common.BasicAuthHelper;
import io.continual.iam.impl.common.HeaderReader;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.nv.NvReadable;

public class ApiContextHelper<I extends Identity>
{
	public ApiContextHelper ()
	{
	}
	
	public static final String kSetting_ContinualProductTag = "apiKeyProductTag";
	public static final String kContinualProductTag = "continual";
	public static final String kContinualSystemsGroup = "continualSystems";

	//
	// API clients sign their requests with this format:
	//
    // kServerNameInSignedContentHeader + "." + dateString [ + apiMagic ]
	//

	public static final String kServerNameInSignedContentHeader = "continual";

	public static final String kSetting_AuthLineHeader = "http.auth.header";
	public static final String kSetting_DateLineHeader = "http.date.header";
	public static final String kSetting_MagicLineHeader = "http.magic.header";

	public static final String kDefault_AuthLineHeader = "X-Continual-Auth";
	public static final String kDefault_DateLineHeader = "X-Continual-Date";
	public static final String kDefault_MagicLineHeader = "X-Continual-Magic";

	public interface ApiHandler<I extends Identity>
	{
		/**
		 * Handle the request as the given user and return a JSON string.
		 * 
		 * @param context the request context
		 * @param servlet the servlet
		 * @param uc the user context
		 * @return a JSON string
		 * @throws IOException 
		 */
		String handle ( CHttpRequestContext context, HttpServlet servlet, UserContext<I> uc ) throws IOException;
	}

	protected static void sendStatusCodeAndMessage ( CHttpRequestContext context, int statusCode, String msg )
	{
		sendJson ( context, statusCode,
			new JSONObject ()
				.put ( "statusCode", statusCode )
				.put ( "message",  msg )
		);
	}

	protected static void sendStatusOk ( CHttpRequestContext context, String msg )
	{
		sendJson ( context, HttpStatusCodes.k200_ok,
			new JSONObject ()
				.put ( "statusCode", HttpStatusCodes.k200_ok )
				.put ( "message",  msg )
		);
	}

	protected static void sendStatusOk ( CHttpRequestContext context, JSONObject msg )
	{
		sendJson ( context, HttpStatusCodes.k200_ok,
			JsonUtil.clone ( msg )
				.put ( "statusCode", HttpStatusCodes.k200_ok )
		);
	}

	protected static void sendStatusOkNoContent ( CHttpRequestContext context )
	{
		context.response ().setStatus ( HttpStatusCodes.k204_noContent );
	}

	protected static void sendNotAuth ( CHttpRequestContext context )
	{
		sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, "Unauthorized. Check your API credentials." );
	}

	protected static void sendJson ( CHttpRequestContext context, JSONObject data )
	{
		sendJson ( context, HttpStatusCodes.k200_ok, data );
	}

	protected static void sendJson ( CHttpRequestContext context, int statusCode, JSONObject data )
	{
		// user can send a header for pretty printed JSON. otherwise we send it in dense form.
		final boolean pretty = TypeConvertor.convertToBooleanBroad ( context.request ().getFirstHeader ( "X-CioPrettyJson" ) );

		context.response ().sendErrorAndBody (
			statusCode,
			( pretty ? data.toString (4) : data.toString () ),
			MimeTypes.kAppJson
		);
	}

	public static <I extends Identity> void handleWithApiAuth ( CHttpRequestContext context, ApiHandler<I> h )
	{
		handleWithApiAuthAndAccess ( context, h );
	}

	public static class ResourceAccess
	{
		public ResourceAccess ( String resourceId, String operation ) { fResId=resourceId; fOp=operation; }

		public final String fResId;
		public final String fOp;
	}
	
	public static <I extends Identity> void handleWithApiAuthAndAccess ( CHttpRequestContext context, ApiHandler<I> h, ResourceAccess... accessReqd )
	{
		try
		{
			final HttpServlet servlet = (HttpServlet) context.getServlet ();
			final IamServiceManager<I,?> am = getAccountsSvc ( context );

			setupCorsHeaders ( context );

			final UserContext<I> user = getUser ( am, context );
			if ( user == null )
			{
				sendNotAuth ( context );
				return;
			}
			
			// check for required access
			for ( ResourceAccess ra : accessReqd )
			{
				if ( !am.getAccessDb().canUser (
						user.getEffectiveUserId (),
						new Resource () { @Override public String getId () { return ra.fResId; } },
						ra.fOp
					) )
				{
					sendNotAuth ( context );
					return;
				}
			}

			// access allowed, call the handler
			final String response = h.handle ( context, servlet, user );
			if ( response != null )
			{
				context.response().getStreamForTextResponse ( MimeTypes.kAppJson ).println ( response );
			}
		}
		catch ( IOException e )
		{
			log.warn ( e.getMessage () );
			context.response ().sendError ( HttpStatusCodes.k500_internalServerError, "I/O problem writing the response, but... you got it???" );
		}
		catch ( JSONException | IamSvcException e )
		{
			log.warn ( e.getMessage (), e );
			context.response ().sendError ( HttpStatusCodes.k500_internalServerError, "There was a problem handling your API request." );
		}
	}

	public static <I extends Identity> UserContext<I> getUser ( final CHttpRequestContext context ) throws IamSvcException
	{
		return getUser ( getAccountsSvc ( context ), context );
	}

	private interface Authenticator<I extends Identity>
	{
		I authenticate ( IamServiceManager<I,?> am, CHttpRequestContext context ) throws IamSvcException;
	}

	private static class LocalHeaderReader implements HeaderReader
	{
		public LocalHeaderReader ( CHttpRequestContext context )
		{
			fContext = context;
		}
		@Override
		public String getFirstHeader ( String header )
		{
			return fContext.request ().getFirstHeader ( header );
		}
		private final CHttpRequestContext fContext;
	}

	private static class AuthList<I extends Identity> implements Authenticator<I>
	{
		public AuthList ()
		{
			fAuthenticators = new LinkedList<>();

			// API key...
			fAuthenticators.add ( new Authenticator<I> ()
			{
				@Override
				public I authenticate ( IamServiceManager<I,?> am, final CHttpRequestContext context ) throws IamSvcException
				{
					final NvReadable ds = context.systemSettings ();

					I authUser = null;
					final ApiKeyCredential creds = ApiKeyAuthHelper.readApiKeyCredential ( ds, new LocalHeaderReader ( context ),
						ds.getString ( kSetting_ContinualProductTag, kContinualProductTag )
					);
					if ( creds != null )
					{
						authUser = am.getIdentityDb ().authenticate ( creds );
						if ( authUser != null ) IamAuthLog.authenticationEvent ( authUser.getId (), "API Key", context.request ().getBestRemoteAddress () );
					}

					return authUser;
				}
			} );

			// JWT...
			fAuthenticators.add ( new Authenticator<I> ()
			{
				@Override
				public I authenticate ( IamServiceManager<I,?> am, final CHttpRequestContext context ) throws IamSvcException
				{
					I authUser = null;
					try
					{
						JwtCredential cred = null;

						// we normally pick up the JWT token from the Auth/bearer header.
						final String authHeader = context.request ().getFirstHeader ( "Authorization" );
						if ( authHeader != null && authHeader.startsWith ( "Bearer " ) )
						{
							final String[] parts = authHeader.split ( " " );
							if ( parts.length == 2 )
							{
								cred = JwtCredential.fromHeader ( authHeader );
							}
						}
						
						// ... but we also support the token as a parameter to support some REST API
						// use cases, like background data loads
						if ( cred == null )
						{
							final String queryParam = context.request ().getParameter ( "jwt", null );
							if ( queryParam != null )
							{
								cred = new JwtCredential ( queryParam );
							}
						}

						if ( cred != null )
						{
							authUser = am.getIdentityDb ().authenticate ( cred );
							if ( authUser != null ) IamAuthLog.authenticationEvent ( authUser.getId (), "JWT", context.request ().getBestRemoteAddress () );
						}
					}
					catch ( InvalidJwtToken e )
					{
						// ignore, can't authenticate this way
					}
					return authUser;
				}
			} );

			// username/password...
			fAuthenticators.add ( new Authenticator<I> ()
			{
				@Override
				public I authenticate ( IamServiceManager<I,?> am, final CHttpRequestContext context ) throws IamSvcException
				{
					final NvReadable ds = context.systemSettings ();

					I authUser = null;
					final UsernamePasswordCredential upc = BasicAuthHelper.readUsernamePasswordCredential ( ds, new LocalHeaderReader ( context ) );
					if ( upc != null )
					{
						authUser = am.getIdentityDb().authenticate ( upc );
						if ( authUser != null ) IamAuthLog.authenticationEvent ( authUser.getId (), "Username/Password", context.request ().getBestRemoteAddress () );
					}
					return authUser;
				}
			} );
		}

		@Override
		public I authenticate ( IamServiceManager<I, ?> am, CHttpRequestContext context ) throws IamSvcException
		{
			for ( Authenticator<I> inner : fAuthenticators )
			{
				I result = inner.authenticate ( am, context );
				if ( result != null ) return result;
			}
			return null;
		}

		private final LinkedList<Authenticator<I>> fAuthenticators;
	}

	public static <I extends Identity> UserContext<I> getUser ( IamServiceManager<I,?> am, final CHttpRequestContext context ) throws IamSvcException
	{
		UserContext<I> result = null;
		I authUser = null;
		try
		{
			// get this user authenticated
			final AuthList<I> al = new AuthList<> ();
			authUser = al.authenticate ( am, context );

			// if we have an authentic user, build a user context
			if ( authUser != null )
			{
				final String authFor = new LocalHeaderReader(context).getFirstHeader ( "X-AuthFor" );
				if ( authFor != null && authFor.length () > 0 && !authFor.equals ( authUser.getId () ) )
				{
					// authorized user is vouching for another...
					
					// get that user's identity
					final I authForUser = am.getIdentityManager ().loadUser( authFor );

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

	/**
	 * The standard accounts service is expected to exist as "accounts"
	 * @param context
	 * @return an account service
	 */
	@SuppressWarnings("unchecked")
	protected static <I extends Identity> IamServiceManager<I,?> getAccountsSvc ( CHttpRequestContext context )
	{
		return HttpServlet.getServices ( context ).get ( "accounts", IamServiceManager.class );
	}

	protected static void setupCorsHeaders ( CHttpRequestContext context )
	{
		final CHttpResponse reply = context.response ();
		reply.writeHeader ( "Access-Control-Allow-Origin", "*");
		reply.writeHeader ( "Access-Control-Allow-Methods", "DELETE, GET, OPTIONS, PATCH, POST, PUT");
		reply.writeHeader ( "Access-Control-Max-Age", "3600");
		reply.writeHeader ( "Access-Control-Allow-Headers",
			"Content-Type, " +
			"Authorization, " +
			ApiContextHelper.kDefault_AuthLineHeader + ", " +
			ApiContextHelper.kDefault_DateLineHeader + ", " +
			ApiContextHelper.kDefault_MagicLineHeader );
	}

	protected static JSONObject readBody ( CHttpRequestContext context ) throws JSONException, IOException
	{
		return new JSONObject ( new CommentedJsonTokener ( context.request().getBodyStream () ) );
	}

	private static final Logger log = LoggerFactory.getLogger ( ApiContextHelper.class );
}
