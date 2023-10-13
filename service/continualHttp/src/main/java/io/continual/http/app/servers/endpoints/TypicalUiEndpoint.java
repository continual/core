package io.continual.http.app.servers.endpoints;

import java.io.IOException;

import org.json.JSONObject;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.sessions.CHttpUserSession;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.UserContext;
import io.continual.services.ServiceContainer;

public class TypicalUiEndpoint<I extends Identity>
{
	public TypicalUiEndpoint ()
	{
	}

	public TypicalUiEndpoint ( ServiceContainer sc, JSONObject config )
	{
		// just for consistency with many other framework classes
	}
	
	/**
	 * No login available
	 */
	public static class NoLoginException extends Exception
	{
		public NoLoginException () { super(); }
		private static final long serialVersionUID = 1L;
	};

	/**
	 * An API handler that's provided the context and an authenticated user.
	 *
	 * @param <I>
	 */
	public interface SessionHandler<I extends Identity>
	{
		/**
		 * Handle the request as the given user and return a JSON string.
		 * 
		 * @param context the request context
		 * @param uc the user context
		 * @throws IOException 
		 */
		void handle ( CHttpRequestContext context, UserContext<I> uc ) throws IOException;
	}

	/**
	 * Handle the given HTTP request with the given user session.
	 * @param context
	 * @param handler
	 * @throws NoLoginException
	 * @throws IOException 
	 */
	public void handleWithUserSession ( CHttpRequestContext context, SessionHandler<I> handler ) throws NoLoginException, IOException 
	{
		handler.handle ( context, getUser ( context ) );
	}

	/**
	 * Get the user associated with the given context.
	 * @param context
	 * @return the user context
	 * @throws NoLoginException if there's no user logged in
	 */
	public UserContext<I> getUser ( final CHttpRequestContext context ) throws NoLoginException
	{
		if ( context.session () != null )
		{
			@SuppressWarnings("unchecked")
			final UserContext<I> ii = (UserContext<I>) CHttpUserSession.getSession ( context ).getUser ();
			if ( ii != null ) return ii;
		}

		throw new NoLoginException ();
	}
}
