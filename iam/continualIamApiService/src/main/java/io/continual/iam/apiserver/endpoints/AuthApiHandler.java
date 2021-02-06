package io.continual.iam.apiserver.endpoints;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.iam.IamServiceManager;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.UserContext;
import io.continual.restHttp.HttpServlet;

/**
 * Auth API
 */
public class AuthApiHandler extends BaseEndpoint
{
	public void login ( CHttpRequestContext context ) throws IamSvcException, IOException
	{
		try
		{
			setupCorsHeaders ( context );

			final JSONObject body = readBody ( context );
			final String username = readJsonString ( body, "username" );
			final String password = readJsonString ( body, "password" );

			final IamServiceManager<?,?> am = HttpServlet.getServices ( context ).get ( "accounts", IamServiceManager.class );
			final Identity ii = am.getIdentityDb ().authenticate ( new UsernamePasswordCredential ( username, password ) );
			if ( ii != null )
			{
				final String token = am.getIdentityDb ().createJwtToken ( ii );
				sendJson ( context, new JSONObject ()
					.put ( "status", "ok" )
					.put ( "token", token )
				);
			}
			else
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, "Unable to sign in."  );
			}
		}
		catch ( MissingInputException x )
		{
			sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, x.getMessage () );
		}
		catch ( JSONException x )
		{
			sendStatusCodeAndMessage ( context, HttpStatusCodes.k400_badRequest, "There's a problem with your JSON." );
		}
	}

	public void logout ( CHttpRequestContext context ) throws IamSvcException, IOException
	{
		setupCorsHeaders ( context );

		final UserContext user = getUser ( context );
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
		setupCorsHeaders ( context );

		final UserContext user = getUser ( context );
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
		sendStatusOk ( context, "ok" );
	}
}
