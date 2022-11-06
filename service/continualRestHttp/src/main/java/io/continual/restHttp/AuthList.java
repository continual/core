package io.continual.restHttp;

import java.util.LinkedList;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.iam.IamAuthLog;
import io.continual.iam.IamServiceManager;
import io.continual.iam.credentials.ApiKeyCredential;
import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.credentials.JwtCredential.InvalidJwtToken;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.ApiKeyAuthHelper;
import io.continual.iam.impl.common.BasicAuthHelper;
import io.continual.restHttp.ApiContextHelper.Authenticator;
import io.continual.restHttp.ApiContextHelper.LocalHeaderReader;
import io.continual.util.nv.NvReadable;

public class AuthList<I extends Identity> implements Authenticator<I>
{
	public static final String kSetting_ContinualProductTag = "apiKeyProductTag";
	public static final String kContinualProductTag = "continual";

	public AuthList ()
	{
		fAuthenticators = new LinkedList<>();

		// API key...
		addAuthenticator ( new Authenticator<I> ()
		{
			@Override
			public I authenticate ( IamServiceManager<I,?> am, final CHttpRequestContext context ) throws IamSvcException
			{
				final NvReadable ds = context.systemSettings ();
				final String systag = ds.getString ( kSetting_ContinualProductTag, kContinualProductTag );

				I authUser = null;
				final ApiKeyCredential creds = ApiKeyAuthHelper.readApiKeyCredential ( ds, new LocalHeaderReader ( context ), systag );
				if ( creds != null )
				{
					authUser = am.getIdentityDb ().authenticate ( creds );
					if ( authUser != null )
					{
						IamAuthLog.authenticationEvent ( authUser.getId (), "API Key", context.request ().getBestRemoteAddress () );
					}
				}

				return authUser;
			}
		} );

		// JWT...
		addAuthenticator ( new Authenticator<I> ()
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
						if ( authUser != null )
						{
							IamAuthLog.authenticationEvent ( authUser.getId (), "JWT", context.request ().getBestRemoteAddress () );
						}
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
		addAuthenticator ( new Authenticator<I> ()
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
					if ( authUser != null )
					{
						IamAuthLog.authenticationEvent ( authUser.getId (), "Username/Password", context.request ().getBestRemoteAddress () );
					}
				}
				return authUser;
			}
		} );
	}

	public synchronized AuthList<I> addAuthenticator ( Authenticator<I> a )
	{
		fAuthenticators.add ( a );
		return this;
	}
	
	@Override
	public synchronized I authenticate ( IamServiceManager<I, ?> am, CHttpRequestContext context ) throws IamSvcException
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
