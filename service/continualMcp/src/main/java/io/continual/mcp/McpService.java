package io.continual.mcp;

import org.json.JSONObject;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.http.app.servers.routeInstallers.TypicalApiServiceRouteInstaller;
import io.continual.http.service.framework.CHttpService;
import io.continual.iam.identity.Identity;
import io.continual.mcp.sessionStore.InMemorySessionStore;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

public class McpService<I extends Identity> extends SimpleService
{
	public static final String kSetting_HttpServiceName = "http";
	public static final String kDefault_HttpServiceName = "http";

	public static final String kSetting_SessionStore = "sessionStore";

	public McpService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fInfo = new McpInfo ();
		fTools = new McpToolRegistry ();

		JSONObject ssConfig = config.optJSONObject ( kSetting_SessionStore );
		if ( ssConfig == null )
		{
			ssConfig = new JSONObject ()
				.put ( "class", InMemorySessionStore.class.getCanonicalName () )
			;
		}
		ssConfig = sc.getExprEval ().evaluateJsonObject ( ssConfig );
		final McpSessionStore sessionStore = Builder.fromJson ( McpSessionStore.class, ssConfig );

		final CHttpService http = sc.getReqd ( config.optString ( kSetting_HttpServiceName, kDefault_HttpServiceName ), CHttpService.class );
		http.addRouteInstaller (
			new TypicalApiServiceRouteInstaller ()
				.registerRoutes ( "mcpRoutes.conf", McpService.class, new McpEndpoints<I> ( sc, config, fInfo, sessionStore, fTools ) )
		);
	}

	public McpInfo getMcpInfo ()
	{
		return fInfo;
	}
	
	public McpToolRegistry getToolRegistry ()
	{
		return fTools;
	}
	
	private final McpInfo fInfo;
	private final McpToolRegistry fTools;
}
