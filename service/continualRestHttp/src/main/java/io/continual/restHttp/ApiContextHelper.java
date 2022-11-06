/*
 *	Copyright 2021, Continual.io
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

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.context.CHttpResponse;
import io.continual.iam.IamServiceManager;
import io.continual.iam.access.AccessDb;
import io.continual.iam.access.Resource;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.UserContext;
import io.continual.iam.impl.common.HeaderReader;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.standards.MimeTypes;

/**
 * Intended as a base or utility class for entry point implementations.
 * 
 * @param <I>
 */
public class ApiContextHelper<I extends Identity>
{
	public ApiContextHelper ()
	{
		this ( null );
	}
	
	public ApiContextHelper ( IamServiceManager<I, ?> accts )
	{
		fAccts = accts;
	}
	
	public static final String kSetting_ContinualProductTag = "apiKeyProductTag";
	public static final String kContinualProductTag = "continual";
	public static final String kContinualSystemsGroup = "continualSystems";

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
		 * @throws IOException 
		 */
		void handle ( CHttpRequestContext context, HttpServlet servlet, UserContext<I> uc ) throws IOException;
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

	public static class ResourceAccess
	{
		public ResourceAccess ( String resourceId, String operation ) { fResId=resourceId; fOp=operation; }

		public final String fResId;
		public final String fOp;
	}

	public void handleWithApiAuth ( CHttpRequestContext context, ApiHandler<I> h )
	{
		handleWithApiAuth ( context, getInternalAccts(context), h );
	}

	public void handleWithApiAuthAndAccess ( CHttpRequestContext context, ApiHandler<I> am, ResourceAccess... accessReqd )
	{
		handleWithApiAuthAndAccess ( context, getInternalAccts(context), am, accessReqd );
	}

	public static <I extends Identity> void handleWithApiAuth ( CHttpRequestContext context, IamServiceManager<I,?> am, ApiHandler<I> h )
	{
		handleWithApiAuthAndAccess ( context, am, h );
	}

	public static <I extends Identity> void handleWithApiAuthAndAccess ( CHttpRequestContext context, IamServiceManager<I,?> am, ApiHandler<I> h, ResourceAccess... accessReqd )
	{
		try
		{
			setupCorsHeaders ( context );

			final UserContext<I> user = getUser ( am, context );
			if ( user == null )
			{
				sendNotAuth ( context );
				return;
			}
	
			// check for required access
			final String uid = user.getEffectiveUserId ();
			final AccessDb<?> adb = am.getAccessDb ();
			for ( ResourceAccess ra : accessReqd )
			{
				if ( !adb.canUser ( uid, makeResource ( ra.fResId ), ra.fOp ) )
				{
					sendNotAuth ( context );
					return;
				}
			}

			// access allowed, call the handler
			h.handle ( context, (HttpServlet) context.getServlet (), user );
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
	 * Authenticate the calling user using the default accounts service (named "accounts") and return a UserContext.
	 * @param <I>
	 * @param context
	 * @return a UserContext or null if the user is not authenticated
	 * @throws IamSvcException
	 */
	public static <I extends Identity> UserContext<I> getUser ( final CHttpRequestContext context ) throws IamSvcException
	{
		return getUser ( getAccountsSvc ( context ), context );
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

	public static Resource makeResource ( String named ) { return new Resource () { @Override public String getId () { return named; } }; }

	public interface Authenticator<I extends Identity>
	{
		I authenticate ( IamServiceManager<I,?> am, CHttpRequestContext context ) throws IamSvcException;
	}

	public static class LocalHeaderReader implements HeaderReader
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

	/**
	 * Authenticate the calling user and return a UserContext.
	 * @param <I>
	 * @param am
	 * @param context
	 * @return a UserContext or null if the user is not authenticated
	 * @throws IamSvcException
	 */
	public static <I extends Identity> UserContext<I> getUser ( IamServiceManager<I,?> am, final CHttpRequestContext context ) throws IamSvcException
	{
		UserContext<I> result = null;
		I authUser = null;
		try
		{
			// get this user authenticated
			@SuppressWarnings("unchecked")
			final AuthList<I> al = AuthListSingleton.getAuthList ();
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

	protected IamServiceManager<I,?> getInternalAccts ( CHttpRequestContext context )
	{
		if ( fAccts != null ) return fAccts;
		return getAccountsSvc ( context );
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

	/**
	 * Read a JSON object body
	 * @param ctx
	 * @return a JSON object 
	 * @throws JSONException
	 * @throws IOException
	 */
	public static JSONObject readJsonBody ( CHttpRequestContext ctx ) throws JSONException, IOException
	{
		return readBody ( ctx );
	}

	public static class MissingInputException extends Exception
	{
		public MissingInputException ( String msg ) { super ( msg ); }
		private static final long serialVersionUID = 1L;
	}

	public static String readJsonString ( JSONObject from, String label ) throws MissingInputException
	{
		try
		{
			return from.getString ( label );
		}
		catch ( JSONException x )
		{
			throw new MissingInputException ( "Missing required field '" + label + "' in input." );
		}
	}
	
	public static JSONObject readJsonObject ( JSONObject from, String label ) throws MissingInputException
	{
		try
		{
			return from.getJSONObject ( label );
		}
		catch ( JSONException x )
		{
			throw new MissingInputException ( "Missing required field '" + label + "' in input." );
		}
	}

	private final IamServiceManager<I, ?> fAccts;

	private static final Logger log = LoggerFactory.getLogger ( ApiContextHelper.class );
}
