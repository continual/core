package io.continual.services.rcvr;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.app.servers.routeInstallers.TypicalApiServiceRouteInstaller;
import io.continual.http.service.framework.CHttpService;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

public class ReceiverApiService<I extends Identity> extends SimpleService
{
	public ReceiverApiService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		final CHttpService http = sc.getReqd ( config.optString ( "httpService", "httpService" ), CHttpService.class );

		http.addRouteInstaller (
			new TypicalApiServiceRouteInstaller ()
				.registerRoutes ( "rcvrRoutes.conf", new ReceiverApi<I> ( sc, config ) )
		);
	}
}
