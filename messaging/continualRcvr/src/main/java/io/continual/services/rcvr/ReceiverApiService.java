package io.continual.services.rcvr;

import java.io.IOException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.http.service.framework.routing.playish.CHttpPlayishInstanceCallRoutingSource;
import io.continual.iam.identity.Identity;
import io.continual.resources.ResourceLoader;
import io.continual.restHttp.BaseApiServiceRouter;
import io.continual.restHttp.HttpService;
import io.continual.restHttp.HttpServlet;
import io.continual.services.Service;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.nv.NvReadable;

public class ReceiverApiService<I extends Identity> extends SimpleService implements Service
{
	public ReceiverApiService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		final HttpService server = sc.get ( config.optString ( "httpService", "httpService" ), HttpService.class );
		server.addRouter (
			"rcvrApi",
			new BaseApiServiceRouter ()
			{
				@Override
				public void setupRouter ( HttpServlet servlet, CHttpRequestRouter rr, NvReadable p ) throws IOException, BuildFailure
				{
					super.setupExceptionHandlers ( servlet, rr, p );

					// setup routes
					for ( String routeFile : new String[]
						{
							"rcvrRoutes.conf"
						} )
					{
						log.debug ( "Loading routes from " + routeFile );
						rr.addRouteSource ( new CHttpPlayishInstanceCallRoutingSource<ReceiverApi<I>> (
							new ReceiverApi<I> ( sc, config ),
							ResourceLoader.load ( routeFile ) )
						);
					}
				}
			}
		);
	}

	private static final Logger log = LoggerFactory.getLogger ( ReceiverApiService.class );
}
