package io.continual.basesvcs.services.http;

import java.io.IOException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.basesvcs.model.user.UserContext;
import io.continual.basesvcs.services.accounts.AccountService;
import io.continual.basesvcs.tools.ApiContextHelper;
import io.continual.http.service.framework.context.DrumlinRequestContext;
import io.continual.iam.identity.Identity;
import io.continual.util.http.standards.HttpStatusCodes;
import io.continual.util.http.standards.MimeTypes;

public class HttpSessionContextHelper
{
	public static class NoLoginException extends Exception
	{
		public NoLoginException () { super(); }
		private static final long serialVersionUID = 1L;
	};

	public interface SessionHandler
	{
		/**
		 * Handle the request as the given user.
		 * 
		 * @param context
		 * @param servlet
		 * @param user
		 */
		void handle ( DrumlinRequestContext context, HttpServlet servlet, Identity user ) throws IOException;
	}

	public static void handleWithUserSession ( DrumlinRequestContext context, SessionHandler h ) throws NoLoginException 
	{
		try
		{
			final HttpServlet servlet = (HttpServlet) context.getServlet ();
			final UserContext user = getUser ( context );
			if ( user != null )
			{
				h.handle ( context, servlet, user.getUser () );
			}
			else
			{
				throw new NoLoginException ( );
			}
		}
		catch ( IOException e )
		{
			log.warn ( e.getMessage () );
			context.response ().sendError ( HttpStatusCodes.k500_internalServerError, "I/O problem writing the response, but... you got it???" );
		}
	}

	public static UserContext getUserNoThrow ( final DrumlinRequestContext context )
	{
		return HttpUserSession.getSession ( context ).getUser ();
	}

	public static UserContext getUser ( final DrumlinRequestContext context ) throws NoLoginException
	{
		if ( context.session () != null )
		{
			final UserContext ii = HttpUserSession.getSession ( context ).getUser ();
			if ( ii != null ) return ii;
		}

		UserContext ii = ApiContextHelper.getUser ( getAccountsSvc(context), context );
		if ( ii == null )
		{
			throw new NoLoginException ();
		}

		return ii;
	}

	protected static void sendJson ( DrumlinRequestContext context, JSONObject data )
	{
		sendJson ( context, HttpStatusCodes.k200_ok, data );
	}

	protected static void sendJson ( DrumlinRequestContext context, int statusCode, JSONObject data )
	{
		context.response ().sendErrorAndBody (
			statusCode,
			data.toString (),
			MimeTypes.kAppJson
		);
	}

	protected static AccountService<?,?> getAccountsSvc ( DrumlinRequestContext context )
	{
		return HttpServlet.getServices ( context ).get ( "accounts", AccountService.class );
	}

//	private static Identity checkLocalDevAccess ( AccountService<?,?> am, DrumlinRequestContext context )
//	{
//		final rrNvReadable ds = context.systemSettings ();
//
//		Identity result = null;
//		final String addr = context.request ().getBestRemoteAddress ();
//		final String signature = context.request ().getFirstHeader ( ds.getString ( kSetting_AuthLineHeader, kDefault_AuthLineHeader ) );
//		final boolean allowed = ds.getBoolean ( kSetting_OtterleyApiDevAccessOn, false );
//
//		if ( allowed && addr.equals ( "127.0.0.1" ) && signature != null &&
//			signature.startsWith ( kSpecialTestUser + ":" ) )
//		{
//			try
//			{
//				if ( !am.userExists ( kSpecialTestUser ) )
//				{
//					am.createUser ( kSpecialTestUser );
//				}
//				result = am.loadUser ( kSpecialTestUser );
//			}
//			catch ( IamSvcException | IamIdentityExists e )
//			{
//				log.warn ( "While trying to grant dev access: " + e.getMessage () );
//			}
//		}
//		else
//		{
//			authLog ( "No local dev access. allowed=" + allowed + ", addr=" + addr + ", sig=" + signature );
//		}
//		return result;
//	}

//	private static final String kSetting_OtterleyApiDevAccessOn = "otterley.api.devaccess";

	private static final Logger log = LoggerFactory.getLogger ( HttpSessionContextHelper.class );

//	private static final boolean skAuthLogging = true;
//	private static void authLog ( String msg )
//	{
//		if ( skAuthLogging )
//		{
//			log.info ( msg );
//		}
//		else
//		{
//			log.debug ( msg );
//		}
//	}
}
