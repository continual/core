package io.continual.services.model.api.endpoints;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.email.impl.SimpleEmailService;
import io.continual.http.app.servers.endpoints.TypicalRestApiEndpoint;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.iam.IamServiceManager;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.IdentityDb;
import io.continual.iam.identity.UserContext;
import io.continual.services.ServiceContainer;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.time.Clock;

/**
 * Auth API
 * @deprecated Identity management should be centralized in the IAM service
 */
@Deprecated
public class AuthApiHandler extends TypicalRestApiEndpoint<Identity>
{
	private static final String kSetting_PwResetLink = "passwordResetLinkBase";
	private static final String kDefault_PwResetLink = "https://docs.cfgex.com/pwr";

	private static final String kSetting_Emailer = "emailer";
	private static final String kDefault_Emailer = "emailer";

	public AuthApiHandler ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config );

		final String baseUrlConfig = config.optString ( kSetting_PwResetLink, kDefault_PwResetLink );
		String baseUrlEvaled = sc.getExprEval ().evaluateText ( baseUrlConfig );
		if ( baseUrlEvaled == null || baseUrlEvaled.length () == 0 )
		{
			log.warn ( "Password reset link config eval'd to an empty string. Using default." );
			baseUrlEvaled = kDefault_PwResetLink;
		}
		fResetUrl = sc.getExprEval ().evaluateText ( config.optString ( kSetting_PwResetLink, kDefault_PwResetLink ) );

		fEmailSvc = sc.getReqd ( sc.getExprEval ().evaluateText ( config.optString ( kSetting_Emailer, kDefault_Emailer ) ), SimpleEmailService.class );

		// we need an accts mgr (vs simple identity db) here for password resets
		fAcctsMgr = sc.getReqd ( getAcctsSvcName ( config ), IamServiceManager.class );
	}

	public void login ( CHttpRequestContext context ) throws IamSvcException, IOException
	{
		try
		{
			final JSONObject body = readBody ( context );
			final String username = body.getString ( "username" );
			final String password = body.getString ( "password" );

			final IdentityDb<Identity> idb = getInternalAccts().getIdentityDb ();
			final Identity ii = idb.authenticate ( new UsernamePasswordCredential ( username, password ) );
			if ( ii != null )
			{
				final String token = idb.createJwtToken ( ii );

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
			final String authHeader = context.request ().getFirstHeader ( "Authorization" );
			if ( authHeader != null && authHeader.startsWith ( "Bearer " ) )
			{
				final String[] parts = authHeader.split ( " " );
				if ( parts.length == 2 )
				{
					final IdentityDb<Identity> idb = getInternalAccts().getIdentityDb ();
					idb.invalidateJwtToken ( parts[1] );
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

		final IdentityDb<Identity> idb = getInternalAccts().getIdentityDb ();
		final Identity ii = idb.authenticate ( new UsernamePasswordCredential ( username, password ) );
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
			final JSONObject body = readBody ( context );
			final String email = body.optString ( "email", null );
			final String tag = body.optString ( "tag", null );
			final String newPassword = body.optString ( "newPassword", null );

			final IdentityDb<Identity> idb = getInternalAccts().getIdentityDb ();

			if ( email != null )
			{
				final Identity ii = idb.loadUser ( email );
				if ( ii != null )
				{
					final String newTag = ii.requestPasswordReset ( 60 * 60 * 24, "cfgex.docs.app." + Clock.now () );
					final String msg = buildResetMsg ( email, newTag );
					fEmailSvc.mail ( email, "Password reset instructions", msg );
				}
			}
			else if ( tag != null && newPassword != null )
			{
				try
				{
					if ( !fAcctsMgr.getIdentityManager ().completePasswordReset ( tag, newPassword ) )
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
	private final SimpleEmailService fEmailSvc;
	private final IamServiceManager<?,?> fAcctsMgr;

	private static final Logger log = LoggerFactory.getLogger ( AuthApiHandler.class );
}
