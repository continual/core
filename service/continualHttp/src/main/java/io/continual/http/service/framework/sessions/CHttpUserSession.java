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

package io.continual.http.service.framework.sessions;

import java.io.ByteArrayInputStream;
import java.util.HashMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.CHttpSession;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.iam.IamService;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.UserContext;
import io.continual.templating.ContinualTemplateContext;

/**
 * A session that captures a user login.
 */
public class CHttpUserSession implements CHttpSession
{
	public static CHttpUserSession getSession ( CHttpRequestContext context )
	{
		final CHttpSession dc = context.session ();
		if ( dc instanceof CHttpUserSession )
		{
			return (CHttpUserSession) dc;
		}
		throw new IllegalArgumentException ( dc == null ? "No user session established." : "User session is not an HttpUserSession" );
	}
	
	public CHttpUserSession ( IamService<?,?> accounts )
	{
		fAccounts = accounts;
		fUser = null;
		fSessionData = new HashMap<>();
	}

	public CHttpUserSession put ( String key, String val )
	{
		fSessionData.put ( key, val );
		return this;
	}

	public CHttpUserSession remove ( String key )
	{
		fSessionData.remove ( key );
		return this;
	}

	public void populateTemplateContext ( ContinualTemplateContext templateCtx )
	{
		templateCtx.putAll ( fSessionData );
	}

	public void login ( String username, String password )
	{
		if ( isLoggedIn () )
		{
			logout ();
		}

		try
		{
			final Identity user = fAccounts != null ? fAccounts.getIdentityDb ().authenticate ( new UsernamePasswordCredential ( username, password ) ) : null;
			if ( user != null )
			{
				log.info ( "LOGIN_OK [" + user.getId() + "]." );
				fUser = new UserContext.Builder<Identity> ().forUser ( user ).build ();
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
		fUser = new UserContext.Builder<Identity> ().forUser ( user ).build ();
	}

	public boolean isLoggedIn ()
	{
		return fUser != null;
	}

	public UserContext<?> getUser ()
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

	private final IamService<?,?> fAccounts;
	private UserContext<?> fUser;
	private HashMap<String,String> fSessionData;
	private static final Logger log = LoggerFactory.getLogger ( CHttpUserSession.class );
}
