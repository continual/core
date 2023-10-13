package io.continual.iam.apiserver;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.app.servers.routeInstallers.TypicalApiServiceRouteInstaller;
import io.continual.http.service.framework.CHttpService;
import io.continual.http.service.framework.routing.playish.CHttpPlayishStaticEntryPointRoutingSource;
import io.continual.iam.apiserver.endpoints.AuthApiHandler;
import io.continual.iam.apiserver.endpoints.IamApiHandler;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

public class IamHttpApiService<I extends Identity> extends SimpleService
{
	public IamHttpApiService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		final CHttpService http = sc.getReqd ( config.getString ( "httpService" ), CHttpService.class );

		http.addRouteInstaller (
			new TypicalApiServiceRouteInstaller ()
				.registerRoutes ( "authRoutes.conf", new AuthApiHandler<I> ( sc, config ) )
				.registerRoutes ( "iamRoutes.conf", new IamApiHandler<I> ( sc, config ) )
				.registerRouteSource (
					new CHttpPlayishStaticEntryPointRoutingSource ()
						.addRoute ( "GET", "/guide", "staticDir:com/rathravane/labels/guide;index.html" )
				)
		);
	}
}
