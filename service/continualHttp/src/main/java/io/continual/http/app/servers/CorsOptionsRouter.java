package io.continual.http.app.servers;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import io.continual.http.app.servers.endpoints.TypicalRestApiEndpoint;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.service.framework.routing.CHttpRouteInvocation;
import io.continual.http.service.framework.routing.CHttpRouteSource;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;
import io.continual.util.standards.HttpStatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorsOptionsRouter implements CHttpRouteSource
{
	public CorsOptionsRouter ( Set<String> allowedOrigins )
	{
		fAllowedOrigins = new TreeSet<> ( allowedOrigins );
	}

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

	/**
	 * Setup CORS headers in the response without an origin check.
	 * @param context the http call context
	 */
	public void setupCorsHeadersWithOrigins ( CHttpRequestContext context )
	{
		setupCorsHeaders ( context, fAllowedOrigins );
	}

	/**
	 * Setup CORS headers in the response without an origin check.
	 * @param context the http call context
	 * @deprecated Use setCorsHeaders with the origin set which may be null
	 */
	@Deprecated
	public static void setupCorsHeaders ( CHttpRequestContext context )
	{
		setupCorsHeaders ( context, null );
	}

	/**
	 * Setup CORS headers in the response based on the allowed origins. If the allowed origins set is null,
	 * the response will allow all origins, but not allow credentials.
	 * @param context the http call context
	 * @param allowedOrigins an optional set of allowed origins, or null to allow all origins without credentials
	 */
	public static void setupCorsHeaders ( CHttpRequestContext context, Set<String> allowedOrigins )
	{
		final boolean withOriginCheck = allowedOrigins != null;
		if ( withOriginCheck )
		{
			final String origin = context.request ().getFirstHeader ( "Origin" );
			if ( origin == null || !allowedOrigins.contains ( origin ) )
			{
				// the origin (if any) is not allowed, so we don't set CORS headers at all
				log.warn ( "Origin {} is not allowed.", origin == null ? "null" : origin );
				return;
			}

			context.response ()
				.writeHeader ( "Access-Control-Allow-Origin", origin )
				.writeHeader ( "Access-Control-Allow-Credentials", "true" )
			;
		}
		else
		{
			context.response ()
				.writeHeader ( "Access-Control-Allow-Origin", "*" )
				// don't write allow-creds
			;
		}

		context.response ()
			.writeHeader ( "Access-Control-Allow-Methods", "DELETE, GET, OPTIONS, PATCH, POST, PUT")
			.writeHeader ( "Access-Control-Max-Age", "3600")
			.writeHeader ( "Access-Control-Allow-Headers", skAllowHeadersValue )
		;
	}

	private static final String skAllowHeadersValue = 
		"Content-Type, " +
		"Authorization, " + 
		TypicalRestApiEndpoint.kDefault_AuthLineHeader + ", " +
		TypicalRestApiEndpoint.kDefault_DateLineHeader + ", " +
		TypicalRestApiEndpoint.kDefault_MagicLineHeader
	;

	private final TreeSet<String> fAllowedOrigins;
	private static final Logger log = LoggerFactory.getLogger ( CorsOptionsRouter.class );
}
