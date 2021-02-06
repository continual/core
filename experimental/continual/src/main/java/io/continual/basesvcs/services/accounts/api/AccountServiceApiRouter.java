package io.continual.basesvcs.services.accounts.api;

import java.io.IOException;
import java.net.URL;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.basesvcs.services.accounts.AccountService;
import io.continual.basesvcs.services.http.BaseApiServiceRouter;
import io.continual.basesvcs.services.http.HttpServlet;
import io.continual.http.service.framework.DrumlinErrorHandler;
import io.continual.http.service.framework.context.DrumlinRequestContext;
import io.continual.http.service.framework.routing.DrumlinRequestRouter;
import io.continual.http.service.framework.routing.playish.DrumlinPlayishInstanceCallRoutingSource;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.services.ServiceContainer;
import io.continual.util.http.standards.HttpStatusCodes;
import io.continual.util.http.standards.MimeTypes;
import io.continual.util.nv.NvReadable;

public class AccountServiceApiRouter extends BaseApiServiceRouter
{
	public AccountServiceApiRouter ( ServiceContainer sc, NvReadable prefs, AccountService<?,?> theService )
	{
		fApi = new AccountServiceApi ( theService );
	}

	@Override
	public void setupRouter ( HttpServlet servlet, DrumlinRequestRouter rr, NvReadable p ) throws IOException
	{
		super.setupExceptionHandlers ( servlet, rr, p );

		// setup routes
		for ( String routeFile : new String[]
			{
				"accountServiceRoutes.conf"
			} )
		{
			log.debug ( "Loading routes from " + routeFile );
			final URL url = this.getClass ().getResource ( routeFile );
			rr.addRouteSource ( new DrumlinPlayishInstanceCallRoutingSource<AccountServiceApi> ( fApi, url ) );
		}

		// catch IAM service outage
		rr.setHandlerForException ( IamSvcException.class,
			new DrumlinErrorHandler ()
			{
				@Override
				public void handle ( DrumlinRequestContext ctx, Throwable cause )
				{
					ctx.response ().sendErrorAndBody ( HttpStatusCodes.k503_serviceUnavailable, 
						new JSONObject ()
							.put ( "error", HttpStatusCodes.k503_serviceUnavailable )
							.put ( "message", cause.getMessage () )
							.toString (),
						MimeTypes.kAppJson );
				}
			} );
	}

	private final AccountServiceApi fApi;

	private static final Logger log = LoggerFactory.getLogger ( AccountServiceApiRouter.class );
}
