package io.continual.basesvcs.services.http;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;

import io.continual.basesvcs.model.user.UserContext;
import io.continual.basesvcs.services.accounts.AccountService;
import io.continual.http.service.framework.DrumlinConnection;
import io.continual.http.service.framework.DrumlinConnectionContext;
import io.continual.http.service.framework.DrumlinServlet;
import io.continual.http.service.framework.context.DrumlinRequestContext;

public class HttpUserSession implements DrumlinConnection
{
	public static HttpUserSession getSession ( DrumlinRequestContext context )
	{
		final DrumlinConnection dc = context.session ();
		if ( dc instanceof HttpUserSession )
		{
			return (HttpUserSession) dc;
		}
		throw new IllegalArgumentException ( dc == null ? "No user session established." : "User session is not an HttpUserSession" );
	}
	
	public HttpUserSession ( AccountService<?,?> accounts )
	{
		fAccounts = accounts;
		fUser = null;
	}

	@Override
	public void onSessionCreate ( DrumlinServlet ws, DrumlinConnectionContext dcc ) throws ServletException
	{
		dcc.setInactiveExpiration ( 14, TimeUnit.DAYS );
	}

	@Override
	public void onSessionClose ()
	{
	}

	@Override
	public void noteActivity ()
	{
	}

	@Override
	public void buildTemplateContext ( HashMap<String, Object> context )
	{
	}

	public void login ( String username, String password )
	{
		if ( isLoggedIn () )
		{
			logout ();
		}

		try
		{
			final Identity user = fAccounts != null ? fAccounts.authenticate ( new UsernamePasswordCredential ( username, password ) ) : null;
			if ( user != null )
			{
				log.info ( "LOGIN_OK [" + user.getId() + "]." );
				fUser = new UserContext.Builder ().forUser ( user ).build ();
			}
			else
			{
				log.info ( "LOGIN_FAIL [" + username + "]" );
			}
		}
		catch ( IamSvcException e )
		{
			log.warn ( "Couldn't login user [" + username + "]: " + e.getMessage(), e );
		}
	}

	public void logout ()
	{
		if ( fUser != null )
		{
			log.info ( "Logout [" + fUser.toString () + "]." );
		}
		fUser = null;
	}

	public void replaceLoggedInUser ( Identity user )
	{
		if ( isLoggedIn () ) logout ();
		log.info ( "LOGIN_REPLACED [" + user.getId() + "]." );
		fUser = new UserContext.Builder ().forUser ( user ).build ();
	}

	public boolean isLoggedIn ()
	{
		return fUser != null;
	}

	public UserContext getUser ()
	{
		return fUser;
	}
	
	@Override
	public ByteArrayInputStream serialize ()
	{
		return new ByteArrayInputStream ( new JSONObject().put ( "user", fUser != null ? fUser.toJson () : null ).toString ().getBytes() );
	}

	@Override
	public void deserialize ( ByteArrayInputStream sessionData )
	{
//		final JSONObject o = JsonUtil.readJsonObject ( sessionData );
//		final JSONObject user = o.optJSONObject ( "user" );
//		if ( user != null )
//		{
//			try
//			{
//				fUser = fAccounts != null ? fAccounts.loadUser ( user.getString ( "identity" ) ) : null;
//			}
//			catch ( IamSvcException e )
//			{
//				log.warn ( "Couldn't load user [" + user + "]. " + e.getMessage(), e );
//				fUser = null;
//			}
//		}
//		else
		// FIXME...
		{
			fUser = null;
		}
	}

	private final AccountService<?,?> fAccounts;
	private UserContext fUser;
	private static final Logger log = LoggerFactory.getLogger ( HttpUserSession.class );
}
