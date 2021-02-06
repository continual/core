package io.continual.basesvcs.services.http;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.DrumlinErrorHandler;
import io.continual.http.service.framework.context.DrumlinRequestContext;
import io.continual.http.service.framework.routing.DrumlinRequestRouter;
import io.continual.util.http.standards.HttpStatusCodes;
import io.continual.util.http.standards.MimeTypes;
import io.continual.util.nv.NvReadable;

public abstract class BaseApiServiceRouter implements HttpRouter
{
	protected void setupExceptionHandlers ( HttpServlet servlet, DrumlinRequestRouter rr, NvReadable p ) throws IOException
	{
		rr.setHandlerForException ( DrumlinRequestRouter.noMatchingRoute.class,
			new DrumlinErrorHandler ()
			{
				@Override
				public void handle ( DrumlinRequestContext ctx, Throwable cause )
				{
					ctx.response ().sendErrorAndBody ( HttpStatusCodes.k404_notFound, 
						new JSONObject ()
							.put ( "error", HttpStatusCodes.k404_notFound )
							.put ( "message", "Not found. See the API docs." )
							.toString (),
						MimeTypes.kAppJson );
				}
			} );

		rr.setHandlerForException ( JSONException.class,
			new DrumlinErrorHandler ()
			{
				@Override
				public void handle ( DrumlinRequestContext ctx, Throwable cause )
				{
					ctx.response ().sendErrorAndBody ( HttpStatusCodes.k400_badRequest, 
						new JSONObject ()
							.put ( "error", HttpStatusCodes.k400_badRequest )
							.put ( "message", "Bad request. See the API docs." )
							.toString (),
						MimeTypes.kAppJson );
				}
			} );

		rr.setHandlerForException ( Throwable.class,
			new DrumlinErrorHandler ()
			{
				@Override
				public void handle ( DrumlinRequestContext ctx, Throwable cause )
				{
					ctx.response ().sendErrorAndBody ( HttpStatusCodes.k500_internalServerError, 
						new JSONObject ()
							.put ( "error", HttpStatusCodes.k500_internalServerError )
							.put ( "message", "There was a problem at the server." )
							.toString (),
						MimeTypes.kAppJson );
					log.warn ( cause.getMessage (), cause );
				}
			} );
	}

	private static final Logger log = LoggerFactory.getLogger ( BaseApiServiceRouter.class );
}
