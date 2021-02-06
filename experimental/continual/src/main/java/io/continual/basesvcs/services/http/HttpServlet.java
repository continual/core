package io.continual.basesvcs.services.http;

import java.io.IOException;
import java.util.LinkedList;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.services.ServiceContainer;
import io.continual.util.nv.NvReadable;
import io.continual.basesvcs.services.accounts.AccountService;
import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.DrumlinServlet;
import io.continual.http.service.framework.context.DrumlinRequestContext;
import io.continual.http.service.framework.routing.DrumlinRequestRouter;

public class HttpServlet extends DrumlinServlet
{
	public HttpServlet ( ServiceContainer sc, AccountService<?,?> accounts, SessionLifeCycle lc )
	{
		super ( lc );

		fAccounts = accounts;
		fContainer = sc;
		fRouters = new LinkedList<HttpRouter> ();
	}

	public void addRouter ( HttpRouter value )
	{
		fRouters.add ( value );
	}

	public ServiceContainer getServices ()
	{
		return fContainer;
	}

	public static HttpServlet getServlet ( DrumlinRequestContext rc )
	{
		final DrumlinServlet ds = rc.getServlet ();
		if ( !( ds instanceof HttpServlet ))
		{
			throw new IllegalStateException ( "This servlet is not an HttpServlet" );
		}
		return ((HttpServlet)ds);
	}
	
	public static ServiceContainer getServices ( DrumlinRequestContext rc )
	{
		final DrumlinServlet ds = rc.getServlet ();
		if ( !( ds instanceof HttpServlet ))
		{
			throw new IllegalStateException ( "This servlet is not an HttpServlet" );
		}
		return ((HttpServlet)ds).getServices ();
	}

	@Override
	public HttpUserSession createSession () throws NvReadable.MissingReqdSettingException
	{
		return new HttpUserSession ( fAccounts );
	}

	@Override
	protected void servletSetup () throws NvReadable.MissingReqdSettingException, ServletException
	{
		try
		{
			final NvReadable p = super.getSettings ();

			// setup request routing
			final DrumlinRequestRouter rr = super.getRequestRouter ();
			for ( HttpRouter router : fRouters )
			{
				router.setupRouter ( this, rr, p );
			}

			log.info ( "The server is ready." );
		}
		catch ( IOException | BuildFailure e )
		{
			throw new ServletException ( e );
		}
	}

	private final ServiceContainer fContainer;
	private final AccountService<?,?> fAccounts;
	private final LinkedList<HttpRouter> fRouters;

	private static final Logger log = LoggerFactory.getLogger ( HttpServlet.class );
	private static final long serialVersionUID = 1L;
}
