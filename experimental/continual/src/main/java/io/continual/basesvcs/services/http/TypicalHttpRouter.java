package io.continual.basesvcs.services.http;

import java.io.IOException;
import java.net.URL;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.basesvcs.services.http.HttpSessionContextHelper.NoLoginException;
import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.DrumlinErrorHandler;
import io.continual.http.service.framework.context.DrumlinRequestContext;
import io.continual.http.service.framework.routing.DrumlinRequestRouter;
import io.continual.http.service.framework.routing.playish.DrumlinPlayishInstanceCallRoutingSource;
import io.continual.http.service.framework.routing.playish.DrumlinPlayishRoutingFileSource;
import io.continual.util.http.standards.HttpStatusCodes;
import io.continual.util.http.standards.MimeTypes;

public abstract class TypicalHttpRouter implements HttpRouter
{
	public class ConfigAndClass<T>
	{
		public ConfigAndClass ( String config, T handlerInstance )
		{
			fConfig = config;
			fHandlerInstance = handlerInstance;
		}
		public final String fConfig;
		public final T fHandlerInstance;
	}

	protected void setupTypicalApiRouter ( DrumlinRequestRouter rr )
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

		rr.setHandlerForException ( NoLoginException.class,
			new DrumlinErrorHandler ()
			{
				@Override
				public void handle ( DrumlinRequestContext ctx, Throwable cause )
				{
					ctx.response ().sendErrorAndBody ( HttpStatusCodes.k401_unauthorized, 
						new JSONObject ()
							.put ( "error", HttpStatusCodes.k401_unauthorized )
							.put ( "message", "Check your credentials." )
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
				}
			} );
	}
	
	protected void setupTypicalUiRouter ( DrumlinRequestRouter rr )
	{
		rr.setHandlerForException ( DrumlinRequestRouter.noMatchingRoute.class,
			new DrumlinErrorHandler ()
			{
				@Override
				public void handle ( DrumlinRequestContext ctx, Throwable cause )
				{
					ctx.response().redirect ( "/" );
				}
			} );

		rr.setHandlerForException ( NoLoginException.class,
			new DrumlinErrorHandler ()
			{
				@Override
				public void handle ( DrumlinRequestContext ctx, Throwable cause )
				{
					ctx.response().redirect ( "/" );
				}
			} );
	}

	public void loadConfigs ( DrumlinRequestRouter rr, ConfigAndClass<?>... configs ) throws IOException, BuildFailure
	{
		for ( ConfigAndClass<?> cc : configs )
		{
			log.debug ( "Loading routes from " + cc.fConfig );
			final URL url = this.getClass ().getResource ( cc.fConfig );
			if ( url != null )
			{
				if ( cc.fHandlerInstance != null )
				{
					rr.addRouteSource ( new DrumlinPlayishInstanceCallRoutingSource<Object> ( cc.fHandlerInstance, url ) );
				}
				else
				{
					rr.addRouteSource ( new DrumlinPlayishRoutingFileSource ( url ) );
				}
			}
			else
			{
				throw new IOException ( "Couldn't load resource " + cc.fConfig );
			}
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( TypicalHttpRouter.class );
}
