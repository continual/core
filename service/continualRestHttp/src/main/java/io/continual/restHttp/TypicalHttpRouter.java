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

package io.continual.restHttp;

import java.io.IOException;
import java.net.URL;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.CHttpErrorHandler;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.http.service.framework.routing.playish.CHttpPlayishInstanceCallRoutingSource;
import io.continual.http.service.framework.routing.playish.CHttpPlayishRoutingFileSource;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.restHttp.HttpSessionContextHelper.NoLoginException;
import io.continual.util.standards.MimeTypes;

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

	protected void setupTypicalApiRouter ( CHttpRequestRouter rr )
	{
		rr.setHandlerForException ( CHttpRequestRouter.noMatchingRoute.class,
			new CHttpErrorHandler ()
			{
				@Override
				public void handle ( CHttpRequestContext ctx, Throwable cause )
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
			new CHttpErrorHandler ()
			{
				@Override
				public void handle ( CHttpRequestContext ctx, Throwable cause )
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
			new CHttpErrorHandler ()
			{
				@Override
				public void handle ( CHttpRequestContext ctx, Throwable cause )
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
	
	protected void setupTypicalUiRouter ( CHttpRequestRouter rr )
	{
		rr.setHandlerForException ( CHttpRequestRouter.noMatchingRoute.class,
			new CHttpErrorHandler ()
			{
				@Override
				public void handle ( CHttpRequestContext ctx, Throwable cause )
				{
					ctx.response().redirect ( "/" );
				}
			} );

		rr.setHandlerForException ( NoLoginException.class,
			new CHttpErrorHandler ()
			{
				@Override
				public void handle ( CHttpRequestContext ctx, Throwable cause )
				{
					ctx.response().redirect ( "/" );
				}
			} );
	}

	public void loadConfigs ( CHttpRequestRouter rr, ConfigAndClass<?>... configs ) throws IOException, BuildFailure
	{
		for ( ConfigAndClass<?> cc : configs )
		{
			log.debug ( "Loading routes from " + cc.fConfig );
			final URL url = this.getClass ().getResource ( cc.fConfig );
			if ( url != null )
			{
				if ( cc.fHandlerInstance != null )
				{
					rr.addRouteSource ( new CHttpPlayishInstanceCallRoutingSource<Object> ( cc.fHandlerInstance, url ) );
				}
				else
				{
					rr.addRouteSource ( new CHttpPlayishRoutingFileSource ( url ) );
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
