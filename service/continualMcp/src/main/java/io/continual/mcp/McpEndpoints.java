package io.continual.mcp;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.app.servers.endpoints.TypicalRestApiEndpoint;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.UserContext;
import io.continual.iam.impl.common.SimpleIdentityReference;
import io.continual.mcp.McpTool.ResponseBlock;
import io.continual.services.ServiceContainer;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.standards.HttpStatusCodes;

public class McpEndpoints<I extends Identity> extends TypicalRestApiEndpoint<I>
{
	// JSON-RPC 2.0 error codes
	private static final int kJsonRpcErr_ParseError     = -32700;
	//private static final int kJsonRpcErr_InvalidRequest = -32600;
	private static final int kJsonRpcErr_MethodNotFound = -32601;
	private static final int kJsonRpcErr_InvalidParams  = -32602;
	//private static final int kJsonRpcErr_InternalError  = -32603;

	// optional config: the public base URL (e.g. "https://api.example.com") for use when this
	// server runs behind a TLS-terminating proxy/ingress that doesn't set forwarding headers
	public static final String kSetting_PublicBaseUrl = "publicBaseUrl";

	public McpEndpoints ( ServiceContainer sc, JSONObject config, McpInfo info, McpToolRegistry tools ) throws BuildFailure
	{
		super ( sc, config );

		fInfo = info;
		fTools = tools;

		String publicBaseUrl = sc.getExprEval ().evaluateText ( config.optString ( kSetting_PublicBaseUrl, null ) );
		if ( publicBaseUrl != null && publicBaseUrl.endsWith ( "/" ) )
		{
			publicBaseUrl = publicBaseUrl.substring ( 0, publicBaseUrl.length () - 1 );
		}
		fPublicBaseUrl = publicBaseUrl;
	}

	/**
	 * handle for GET /.well-known/oauth-protected-resource (RFC 8414)
	 * @param ctx
	 */
	public void oauthProtectedResourceMetadata ( CHttpRequestContext ctx )
	{
		if ( !fInfo.hasAuthInfo () )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k404_notFound, "not found" );
			return;
		}

		try
		{
			final String resource = derivePublicUrl ( ctx, "/mcp" );

			sendJson ( ctx, new JSONObject ()
				.put ( "resource", resource )
				.put ( "authorization_servers", new JSONArray ().put ( fInfo.getAuthServer () ) )
				.put ( "scopes_supported", JsonVisitor.listToArray ( fInfo.getAuthScopes () ) )
				.put ( "bearer_methods_supported", new JSONArray().put ( "header" ) )
			);
		}
		catch ( URISyntaxException x )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k500_internalServerError, x.getMessage () );
		}
	}

	/**
	 * This is a stateless interface; no streaming setup needed
	 * @param context
	 */
	public void getMcp ( CHttpRequestContext context )
	{
		sendStatusCodeAndMessage ( context, HttpStatusCodes.k405_methodNotAllowed, "no stateful setup" );
	}

	/**
	 * handle for POST /mcp (stateless HTTP transport)
	 */
	public void postMcpStateless ( CHttpRequestContext context )
	{
		final UserContext<I> user = buildUser ( context );
		if ( user == null ) return;

		try
		{
			final JSONObject body = readBody ( context );
			log.info ( "MCP recd POST [{}]", body.toString () );

			final Object correlationId = body.opt ( "id" );
			final String method = body.optString ( "method", "" );
			final JSONObject params = body.optJSONObject ( "params" );

			JSONObject responseObj = null;

			switch ( method )
			{
				case "notifications/initialized":
					context.response ()
						.setStatus ( HttpStatusCodes.k202_accepted )
					;
					return;

				case "ping":
					responseObj = jsonRpcResult ( correlationId, new JSONObject() );
					break;

				case "initialize":
				{
					String clientPv = kMinPv;
					if ( params != null )
					{
						clientPv = params.optString ( "protocolVersion", kMinPv );
					}
					final String serverPv = calcServerPv ( clientPv );

					final JSONObject result = new JSONObject ()
						.put ( "protocolVersion", serverPv )
						.put ( "serverInfo", new JSONObject ()
							.put ( "name", fInfo.getName () != null ? fInfo.getName () : "Continual MCP Server" )
							.put ( "version", fInfo.getVersion () != null ? fInfo.getVersion () : "1.0.0" )
						)
						.put ( "capabilities", new JSONObject ()
							.put ( "tools", new JSONObject ()
								.put ( "listChanged", false )
							)
						)
					;
					final String instructions = fInfo.getInstructions ();
					if ( !instructions.isEmpty () )
					{
						result.put ( "instructions", instructions );
					}

					responseObj = jsonRpcResult ( correlationId, result );
					break;
				}

				case "tools/list":
				{
					final JSONArray tools = new JSONArray ();
					for ( McpTool tool : fTools.getTools () )
					{
						final JSONObject schema = tool.getInputSchema ();
						if ( schema == null )
						{
							log.warn ( "Tool {} is missing an input schema. Skipped it.", tool.getName () );
							continue;
						}

						tools.put ( new JSONObject ()
							.put ( "name", tool.getName () )
							.put ( "description", tool.getDescription () )
							.put ( "inputSchema", schema )
						);
					}
					responseObj = jsonRpcResult ( correlationId, new JSONObject ().put ( "tools", tools ) );
					break;
				}

				case "tools/call":
				{
					final String toolName = params != null ? params.optString ( "name", null ) : null;
					if ( toolName == null )
					{
						responseObj = jsonRpcError ( correlationId, kJsonRpcErr_InvalidParams, "missing tool name" );
						break;
					}
					final McpTool tool = fTools.getTool ( toolName );
					if ( tool == null )
					{
						responseObj = jsonRpcError ( correlationId, kJsonRpcErr_InvalidParams, "unknown tool: " + toolName );
						break;
					}
					final JSONObject args = params.optJSONObject ( "arguments" );
					try
					{
						final List<ResponseBlock> response = tool.call ( user, args != null ? args : new JSONObject () );
						
						responseObj = jsonRpcResult ( correlationId, new JSONObject ()
							.put ( "content", JsonVisitor.collectionToArray ( response ) )
							.put ( "isError", false )
						);
					}
					catch ( McpTool.McpToolException x )
					{
						final JSONArray content = new JSONArray ()
							.put ( new JSONObject ()
								.put ( "type", "text" )
								.put ( "text", x.getCause () != null ? x.getCause ().getMessage () : x.getMessage () )
							);
						responseObj = jsonRpcResult ( correlationId, new JSONObject ()
							.put ( "content", content )
							.put ( "isError", true )
						);
					}
					break;
				}

				default:
					responseObj = jsonRpcError ( correlationId, kJsonRpcErr_MethodNotFound, "method not found: " + method );
					break;
			}

			if ( responseObj != null )
			{
				sendJson ( context, responseObj );
			}
			else
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k200_ok, "OK" );
			}
		}
		catch ( JSONException | IOException x )
		{
			final JSONObject err = jsonRpcError ( null, kJsonRpcErr_ParseError, "parse error: " + x.getMessage () );
			sendJson ( context, err );
		}
	}

	private void respond401 ( CHttpRequestContext context, boolean hadToken )
	{
		try
		{
			final StringBuilder sb = new StringBuilder ()
				.append ( "Bearer resource_metadata=\"" )
				.append ( deriveUnAuthUrl ( context ) )
				.append ( "\"" )
				.append ( " scope=\"openid profile email\"" )
			;
			if ( hadToken )
			{
				sb.append ( " error=\"invalid_token\"" );
			}

			context.response ().writeHeader ( "WWW-Authenticate", sb.toString () );
			sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, "Unauthorized" );
		}
		catch ( URISyntaxException x )
		{
			sendStatusCodeAndMessage ( context, HttpStatusCodes.k500_internalServerError, x.getMessage () );
		}
	}
	
	@SuppressWarnings("unchecked")
	private UserContext<I> buildUser ( CHttpRequestContext context )
	{
		// if we're not running auth, just assume an anonymous user
		if ( !fInfo.enableAuthFlowEndpoints () )
		{
			return (UserContext<I>) UserContext.builder ().forUser ( new SimpleIdentityReference ( "anonymous" ) ).build ();
		}

		try
		{
			// get the bearer token
			final String authHeader = context.request ().getFirstHeader ( "Authorization" );
			if ( authHeader == null || !authHeader.startsWith ( "Bearer " ) )
			{
				respond401 ( context, false );
				return null;
			}

			// authenticate with the token
			final String token = authHeader.substring ( 7 ).trim ();
			final UserContext<I> user = getUser ( token );
			if ( user == null )
			{
				respond401 ( context, true );
				return null;
			}

			// return the user record
			return user;
		}
		catch ( IamSvcException x )
		{
			sendStatusCodeAndMessage ( context, HttpStatusCodes.k500_internalServerError, x.getMessage () );
			return null;
		}
	}

	// a reverse-ordered list of protocol versions
	private static final String[] kPvs = new String[] { /*"2025-06-18",*/ "2025-03-26"/*, "2024-11-05"*/ };
	private static final String kMinPv = kPvs [ kPvs.length - 1 ];

	private String calcServerPv ( String clientPv )
	{
		// kPvs is reverse-ordered, so the first entry at or before clientPv is the highest match
		for ( String pv : kPvs )
		{
			if ( pv.compareTo ( clientPv ) <= 0 )
			{
				return pv;
			}
		}
		return kMinPv;
	}

	private String deriveUnAuthUrl ( CHttpRequestContext ctx ) throws URISyntaxException
	{
		return derivePublicUrl ( ctx, "/.well-known/oauth-protected-resource" );
	}

	/**
	 * Derive the public-facing URL for a path on this server. When behind a reverse proxy or
	 * ingress that terminates TLS, the request URL reflects the internal (e.g. http) scheme,
	 * so prefer, in order: the configured public base URL, then X-Forwarded-Proto/Host headers,
	 * then the request URL itself.
	 */
	private String derivePublicUrl ( CHttpRequestContext ctx, String path ) throws URISyntaxException
	{
		if ( fPublicBaseUrl != null && !fPublicBaseUrl.isEmpty () )
		{
			return fPublicBaseUrl + path;
		}

		final URI req = new URI ( ctx.request ().getUrl () );

		String scheme = firstListValue ( ctx.request ().getFirstHeader ( "X-Forwarded-Proto" ) );
		if ( scheme == null ) scheme = req.getScheme ();

		String authority = firstListValue ( ctx.request ().getFirstHeader ( "X-Forwarded-Host" ) );
		if ( authority == null ) authority = req.getAuthority ();

		return new URI ( scheme, authority, path, null, null ).toString ();
	}

	// forwarding headers can be comma-separated lists; the first entry is the original client value
	private static String firstListValue ( String headerValue )
	{
		if ( headerValue == null ) return null;
		final int comma = headerValue.indexOf ( ',' );
		final String first = ( comma >= 0 ? headerValue.substring ( 0, comma ) : headerValue ).trim ();
		return first.isEmpty () ? null : first;
	}

	private static JSONObject jsonRpcResult ( Object correlationId, JSONObject result )
	{
		final JSONObject r = new JSONObject ()
			.put ( "jsonrpc", "2.0" )
			.put ( "result", result );
		if ( correlationId != null ) r.put ( "id", correlationId );
		return r;
	}

	private static JSONObject jsonRpcError ( Object correlationId, int code, String message )
	{
		final JSONObject r = new JSONObject ()
			.put ( "jsonrpc", "2.0" )
			.put ( "error", new JSONObject ()
				.put ( "code", code )
				.put ( "message", message )
			);
		if ( correlationId != null ) r.put ( "id", correlationId );
		return r;
	}

	private final McpInfo fInfo;
	private final McpToolRegistry fTools;
	private final String fPublicBaseUrl;

	private static final Logger log = LoggerFactory.getLogger ( McpEndpoints.class );
}
