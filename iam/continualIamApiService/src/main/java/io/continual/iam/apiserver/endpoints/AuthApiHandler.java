package io.continual.iam.apiserver.endpoints;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.app.servers.CorsOptionsRouter;
import io.continual.http.app.servers.endpoints.TypicalRestApiEndpoint;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.iam.IamService;
import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.credentials.JwtCredential.InvalidJwtToken;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.UserContext;
import io.continual.services.ServiceContainer;
import io.continual.util.standards.HttpStatusCodes;

/**
 * Auth API
 */
public class AuthApiHandler<I extends Identity> extends TypicalRestApiEndpoint<I>
{
	public AuthApiHandler ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config );
	}

	public void login ( CHttpRequestContext context ) throws IamSvcException, IOException
	{
		try
		{
			writeCorsHeaders ( context );

			final JSONObject body = readBody ( context );
			final String username = readJsonString ( body, "username" );
			final String password = readJsonString ( body, "password" );

			final IamService<?,?> am = super.getInternalAccts ();
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
		writeCorsHeaders ( context );

		final UserContext<I> user = getUser ( context );
		if ( user != null )
		{
			final IamService<?,?> am = super.getInternalAccts ();

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

	public void validateToken ( CHttpRequestContext context ) throws IamSvcException, IOException
	{
		writeCorsHeaders ( context );

		try
		{
			final String authHeader = context.request ().getFirstHeader ( "Authorization" );
			if ( authHeader != null && authHeader.startsWith ( "Bearer " ) )
			{
				final String[] parts = authHeader.split ( " " );
				if ( parts.length == 2 )
				{
					final JwtCredential token = new JwtCredential ( parts[1] );
					final IamService<?,?> am = super.getInternalAccts ();
					final Identity ii = am.getIdentityDb ().authenticate ( token );
					if ( ii != null )
					{
						sendStatusOk ( context, "token is valid"  );
						return;
					}
				}
			}
			sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, "Couldn't validate your token."  );
		}
		catch ( InvalidJwtToken e )
		{
			sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, "Couldn't validate your token."  );
		}
		catch ( IamSvcException e )
		{
			sendStatusCodeAndMessage ( context, HttpStatusCodes.k503_serviceUnavailable, "There was an error in the auth service."  );
			log.warn ( e.getMessage (), e );
		}
	}

	public void changePassword ( CHttpRequestContext context ) throws IamSvcException, IOException
	{
		writeCorsHeaders ( context );

		final UserContext<I> user = getUser ( context );
		if ( user == null )
		{
			sendNotAuth ( context );
			return;
		}

		final JSONObject body = readBody ( context );

		final String username = user.getEffectiveUserId ();
		final String password = body.getString ( "currentPassword" );
		final String newPassword = body.getString ( "newPassword" );

		final IamService<?,?> am = super.getInternalAccts ();
		final Identity ii = am.getIdentityDb ().authenticate ( new UsernamePasswordCredential ( username, password ) );
		if ( ii == null )
		{
			sendNotAuth ( context );
			return;
		}

		ii.setPassword ( newPassword );
		sendStatusOk ( context, "ok" );
	}

	private static final Logger log = LoggerFactory.getLogger ( AuthApiHandler.class );
}
