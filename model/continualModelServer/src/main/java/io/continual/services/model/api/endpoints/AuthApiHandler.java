package io.continual.services.model.api.endpoints;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.email.impl.SimpleEmailService;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.iam.IamServiceManager;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.UserContext;
import io.continual.restHttp.ApiContextHelper;
import io.continual.restHttp.HttpServlet;
import io.continual.services.ServiceContainer;
import io.continual.util.nv.NvReadable;
import io.continual.util.time.Clock;
import io.continual.util.standards.HttpStatusCodes;

/**
 * Auth API
 */
public class AuthApiHandler extends ApiContextHelper<Identity>
{
	private static final String kSetting_PwResetLink = "passwordResetLinkBase";
	private static final String kDefault_PwResetLink = "https://docs.cfgex.com/pwr";

	public AuthApiHandler ( ServiceContainer sc, JSONObject config )
	{
		final String baseUrlConfig = config.optString ( kSetting_PwResetLink, kDefault_PwResetLink );
		String baseUrlEvaled = sc.getExprEval ().evaluateText ( baseUrlConfig );
		if ( baseUrlEvaled == null || baseUrlEvaled.length () == 0 )
		{
			log.warn ( "Password reset link config eval'd to an empty string. Using default." );
			baseUrlEvaled = kDefault_PwResetLink;
		}
		fResetUrl = sc.getExprEval ().evaluateText ( config.optString ( kSetting_PwResetLink, kDefault_PwResetLink ) );
	}

	public AuthApiHandler ( ServiceContainer sc, NvReadable config )
	{
		final String baseUrlConfig = config.getString ( kSetting_PwResetLink, kDefault_PwResetLink );
		String baseUrlEvaled = sc.getExprEval ().evaluateText ( baseUrlConfig );
		if ( baseUrlEvaled == null || baseUrlEvaled.length () == 0 )
		{
			log.warn ( "Password reset link config eval'd to an empty string. Using default." );
			baseUrlEvaled = kDefault_PwResetLink;
		}
		fResetUrl = sc.getExprEval ().evaluateText ( config.getString ( kSetting_PwResetLink, kDefault_PwResetLink ) );
	}

	public void login ( CHttpRequestContext context ) throws IamSvcException, IOException
	{
		try
		{
			final JSONObject body = readBody ( context );
			final String username = body.getString ( "username" );
			final String password = body.getString ( "password" );

			final IamServiceManager<?,?> am = HttpServlet.getServices ( context ).get ( "accounts", IamServiceManager.class );
			final Identity ii = am.getIdentityDb ().authenticate ( new UsernamePasswordCredential ( username, password ) );
			if ( ii != null )
			{
				final String token = am.getIdentityDb ().createJwtToken ( ii );

				sendJson ( context, new JSONObject ()
					.put ( "status", "ok" )
					.put ( "token", token )
					.put ( "username", username )
				);
			}
			else
			{
				sendJson ( context, HttpStatusCodes.k401_unauthorized, new JSONObject().put ( "message", "Unable to sign in." ) );
			}
		}
		catch ( JSONException x )
		{
			sendJson ( context, HttpStatusCodes.k400_badRequest, new JSONObject().put ( "message", "There's a problem with your JSON." ) );
		}
	}

	public void logout ( CHttpRequestContext context ) throws IamSvcException, IOException
	{
		final UserContext<Identity> user = getUser ( context );
		if ( user != null )
		{
			final IamServiceManager<?,?> am = HttpServlet.getServices ( context ).get ( "accounts", IamServiceManager.class );

			final String authHeader = context.request ().getFirstHeader ( "Authorization" );
			if ( authHeader != null && authHeader.startsWith ( "Bearer " ) )
			{
				final String[] parts = authHeader.split ( " " );
				if ( parts.length == 2 )
				{
					am.getIdentityDb ().invalidateJwtToken ( parts[1] );
				}
			}
		}
	}

	public void changePassword ( CHttpRequestContext context ) throws IamSvcException, IOException
	{
		final UserContext<Identity> user = getUser ( context );
		if ( user == null )
		{
			sendNotAuth ( context );
			return;
		}

		final JSONObject body = readBody ( context );

		final String username = user.getEffectiveUserId ();
		final String password = body.getString ( "currentPassword" );
		final String newPassword = body.getString ( "newPassword" );

		final IamServiceManager<?,?> am = HttpServlet.getServices ( context ).get ( "accounts", IamServiceManager.class );
		final Identity ii = am.getIdentityDb ().authenticate ( new UsernamePasswordCredential ( username, password ) );
		if ( ii == null )
		{
			sendNotAuth ( context );
			return;
		}

		ii.setPassword ( newPassword );
		sendJson ( context, new JSONObject ()
			.put ( "status", "ok" )
		);
	}

	public void passwordResetProcess ( CHttpRequestContext context ) throws IOException
	{
		try
		{
			final IamServiceManager<?,?> am = HttpServlet.getServices ( context ).get ( "accounts", IamServiceManager.class );
			final SimpleEmailService emailSvc = HttpServlet.getServices ( context ).get ( "emailer", SimpleEmailService.class );

			final JSONObject body = readBody ( context );
			final String email = body.optString ( "email", null );
			final String tag = body.optString ( "tag", null );
			final String newPassword = body.optString ( "newPassword", null );

			if ( email != null )
			{
				final Identity ii = am.getIdentityManager ().loadUser ( email );
				if ( ii != null )
				{
					final String newTag = ii.requestPasswordReset ( 60 * 60 * 24, "cfgex.docs.app." + Clock.now () );
					final String msg = buildResetMsg ( email, newTag );
					emailSvc.mail ( email, "Password reset instructions", msg );
				}
			}
			else if ( tag != null && newPassword != null )
			{
				try
				{
					if ( !am.getIdentityManager ().completePasswordReset ( tag, newPassword ) )
					{
						sendJson ( context, HttpStatusCodes.k400_badRequest, new JSONObject ().put ( "status", "error" ) );
						return;
					}
					// else: ok reply
				}
				catch ( IamSvcException x )
				{
					sendJson ( context, HttpStatusCodes.k500_internalServerError, new JSONObject ().put ( "status", "error" ) );
					return;
				}
			}
			
			// else: nothing to do
		}
		catch ( IamSvcException | IamBadRequestException x )
		{
			log.warn ( "starting password reset: " + x.getMessage () );
		}

		// always send ok
		sendJson ( context, new JSONObject ()
			.put ( "status", "ok" )
		);
	}

	private String buildResetMsg ( String email, String tag )
	{
		final StringBuilder sb = new StringBuilder ();
		sb
			.append ( "\n" )
			.append ( "Hi-\n" )
			.append ( "\n" )
			.append ( "We received a request to reset the password for your account. If this was your request,\n" )
			.append ( "please click this link to continue: " )
			.append ( fResetUrl + "?tag=" + tag )
			.append ( "\n" )
			.append ( "\n" )
			.append ( "Thanks!\n" )
			.append ( "\n" )
		;
		return sb.toString ();
	}

	private final String fResetUrl;

	private static final Logger log = LoggerFactory.getLogger ( AuthApiHandler.class );
}
