package io.continual.http.app.servers;

import java.util.Map;

import io.continual.http.app.servers.endpoints.TypicalRestApiEndpoint;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.routing.CHttpRouteInvocation;
import io.continual.http.service.framework.routing.CHttpRouteSource;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;
import io.continual.util.standards.HttpStatusCodes;

public class CorsOptionsRouter implements CHttpRouteSource
{
	@Override
	public CHttpRouteInvocation getRouteFor ( String verb, String path )
	{
		if ( verb != null && verb.equalsIgnoreCase ( "OPTIONS" ) )
		{
			return new CHttpRouteInvocation ()
			{
				@Override
				public void run ( CHttpRequestContext context )
				{
					setupCorsHeaders ( context );
					context.response ().setStatus ( HttpStatusCodes.k204_noContent );
				}

				@Override
				public Path getRouteNameForMetrics ()
				{
					// FIXME: simple implementation for interface compliance
					return Path.getRootPath ().makeChildItem ( Name.fromString ( "options" ) );
				}
			};
		}
		return null;
	}

	@Override
	public String getRouteTo ( Class<?> c, String staticMethodName, Map<String, Object> args )
	{
		return null;
	}
	
	public static void setupCorsHeaders ( CHttpRequestContext context )
	{
		context.response ()
			.writeHeader ( "Access-Control-Allow-Origin", "*")
			.writeHeader ( "Access-Control-Allow-Methods", "DELETE, GET, OPTIONS, PATCH, POST, PUT")
			.writeHeader ( "Access-Control-Max-Age", "3600")
			.writeHeader ( "Access-Control-Allow-Headers", skAllowHeadersValue )
			.writeHeader ( "Access-Control-Allow-Credentials", "true" )
		;
	}

	private static final String skAllowHeadersValue = 
		"Content-Type, " +
		"Authorization, " + 
		TypicalRestApiEndpoint.kDefault_AuthLineHeader + ", " +
		TypicalRestApiEndpoint.kDefault_DateLineHeader + ", " +
		TypicalRestApiEndpoint.kDefault_MagicLineHeader
	;
}
