package io.continual.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.app.servers.endpoints.TypicalRestApiEndpoint;
import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.iam.identity.Identity;
import io.continual.mcp.McpSessionStore.Session;
import io.continual.mcp.McpTool.ResponseBlock;
import io.continual.services.ServiceContainer;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.standards.HttpStatusCodes;
import io.continual.util.time.Clock;

public class McpEndpoints<I extends Identity> extends TypicalRestApiEndpoint<I>
{
	// JSON-RPC 2.0 error codes
	private static final int kJsonRpcErr_ParseError     = -32700;
	//private static final int kJsonRpcErr_InvalidRequest = -32600;
	private static final int kJsonRpcErr_MethodNotFound = -32601;
	private static final int kJsonRpcErr_InvalidParams  = -32602;
	//private static final int kJsonRpcErr_InternalError  = -32603;

	public McpEndpoints ( ServiceContainer sc, JSONObject config, McpInfo info, McpSessionStore sessions, McpToolRegistry tools ) throws BuildFailure
	{
		super ( sc, config );

		fInfo = info;
		fTools = tools;
		fSessions = sessions;
	}

	/**
	 * handle for GET /.well-known/oauth-authorization-server (RFC 8414)
	 * @param ctx
	 */
	public void oauthAuthorizationServerMetadata ( CHttpRequestContext ctx )
	{
		if ( !fInfo.hasAuthInfo () )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k404_notFound, "not found" );
			return;
		}

		try
		{
			final URI req = getOriginalUri ( ctx );
			final String base = new URI ( req.getScheme (), req.getAuthority (), "", null, null ).toString ();

			sendJson ( ctx, new JSONObject ()
				.put ( "issuer", "https://" + fInfo.getAuthDomain () )
				.put ( "authorization_endpoint", base + "/authorize" )
				.put ( "token_endpoint", base + "/token" )
				.put ( "registration_endpoint", base + "/register" )
				.put ( "response_types_supported", new JSONArray ().put ( "code" ) )
				.put ( "grant_types_supported", new JSONArray ().put ( "authorization_code" ) )
				.put ( "token_endpoint_auth_methods_supported", new JSONArray ().put ( "client_secret_post" ) )
			);
		}
		catch ( URISyntaxException x )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k500_internalServerError, x.getMessage () );
		}
	}

	/**
	 * handle for GET /.well-known/oauth-protected-resource/mcp (RFC 8414)
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
			final URI req = getOriginalUri ( ctx );
			final String base = new URI ( req.getScheme (), req.getAuthority (), "", null, null ).toString ();
			final String resource = new URI ( req.getScheme (), req.getAuthority (), "/mcp", null, null ).toString ();

			sendJson ( ctx, new JSONObject ()
				.put ( "resource", resource )
				.put ( "authorization_servers", new JSONArray ().put ( base ) )
			);
		}
		catch ( URISyntaxException x )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k500_internalServerError, x.getMessage () );
		}
	}

	/**
	 * handle for POST /register (RFC 7591)
	 */
	public void registerClient ( CHttpRequestContext ctx )
	{
		try
		{
			final JSONObject req = readBody ( ctx );
			final JSONArray redirectUris = req != null ? req.optJSONArray ( "redirect_uris" ) : null;
			sendJson ( ctx, new JSONObject ()
				.put ( "client_id", "mcp-remote-client" )
				.put ( "redirect_uris", redirectUris != null ? redirectUris : new JSONArray () )
			);
		}
		catch ( Exception e )
		{
			sendJson ( ctx, new JSONObject ()
				.put ( "client_id", "mcp-remote-client" )
				.put ( "redirect_uris", new JSONArray () )
			);
		}
	}

	/**
	 * handle for GET /authorize — returns JSON {state, authUrl} for the MCP client to open
	 * in a system browser and then poll /token?state=... until the token arrives.
	 * @param ctx
	 */
	public void authorize ( CHttpRequestContext ctx )
	{
		if ( !fInfo.enableAuthFlowEndpoints () )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k404_notFound, "not found" );
			return;
		}

		try
		{
			final String clientRedirectUri = ctx.request ().getParameter ( "redirect_uri", null );
			final String clientState = ctx.request ().getParameter ( "state", null );

			final String state = UUID.randomUUID ().toString ();
			fPendingAuths.put ( state, new PendingAuth ( clientRedirectUri, clientState ) );

			final String callbackUri = deriveCallbackUri ( ctx );
			final StringBuilder authUrl = new StringBuilder ()
				.append ( "https://" ).append ( fInfo.getAuthDomain () ).append ( "/authorize" )
				.append ( "?response_type=code" )
				.append ( "&client_id=" ).append ( URLEncoder.encode ( fInfo.getAuthClientId (), StandardCharsets.UTF_8 ) )
				.append ( "&redirect_uri=" ).append ( URLEncoder.encode ( callbackUri, StandardCharsets.UTF_8 ) )
				.append ( "&scope=" ).append ( URLEncoder.encode ( "openid profile email", StandardCharsets.UTF_8 ) )
				.append ( "&state=" ).append ( state )
			;
			if ( fInfo.getAuthAudience () != null )
			{
				authUrl.append ( "&audience=" ).append ( URLEncoder.encode ( fInfo.getAuthAudience (), StandardCharsets.UTF_8 ) );
			}

			ctx.response ().redirectExactly ( authUrl.toString () );
		}
		catch ( URISyntaxException x )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k500_internalServerError, x.getMessage () );
		}
	}

	/**
	 * handle for GET /callback — called by the browser after Auth0 redirects back.
	 * Exchanges the code for a token, stores it keyed by state for /token polling,
	 * and shows the user a completion page.
	 * @param ctx
	 */
	public void callback ( CHttpRequestContext ctx )
	{
		if ( !fInfo.enableAuthFlowEndpoints () )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k404_notFound, "not found" );
			return;
		}

		final String code = ctx.request ().getParameter ( "code", null );
		final String state = ctx.request ().getParameter ( "state", null );

		if ( code == null || state == null )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k400_badRequest, "missing code or state" );
			return;
		}

		final PendingAuth pending = fPendingAuths.get ( state );
		if ( pending == null || pending.isExpired () )
		{
			fPendingAuths.remove ( state );
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k400_badRequest, "unknown or expired state" );
			return;
		}

		try
		{
			final String callbackUri = deriveCallbackUri ( ctx );

			final JSONObject tokenRequestBody = new JSONObject ()
				.put ( "grant_type", "authorization_code" )
				.put ( "client_id", fInfo.getAuthClientId () )
				.put ( "client_secret", fInfo.getAuthClientSecret () )
				.put ( "code", code )
				.put ( "redirect_uri", callbackUri )
			;
			if ( fInfo.getAuthAudience () != null )
			{
				tokenRequestBody.put ( "audience", fInfo.getAuthAudience () );
			}

			@SuppressWarnings("deprecation")
			final URL tokenUrl = new URL ( "https://" + fInfo.getAuthDomain () + "/oauth/token" );
			final HttpURLConnection conn = (HttpURLConnection) tokenUrl.openConnection ();
			conn.setRequestMethod ( "POST" );
			conn.setRequestProperty ( "Content-Type", "application/json" );
			conn.setDoOutput ( true );

			try ( final OutputStream os = conn.getOutputStream () )
			{
				os.write ( tokenRequestBody.toString ().getBytes ( StandardCharsets.UTF_8 ) );
			}

			final int httpStatus = conn.getResponseCode ();
			final InputStream responseStream = httpStatus < 400 ? conn.getInputStream () : conn.getErrorStream ();
			final String responseBody;
			try ( responseStream )
			{
				responseBody = new String ( responseStream.readAllBytes (), StandardCharsets.UTF_8 );
			}

			if ( httpStatus == HttpStatusCodes.k200_ok )
			{
				final JSONObject tokenObj = new JSONObject ( responseBody );
				pending.complete ( tokenObj );

				if ( pending.fClientRedirectUri != null )
				{
					String ourCode = UUID.randomUUID().toString();
					fCodeToPending.put(ourCode, pending);
					ctx.response().redirectExactly( pending.fClientRedirectUri + "?code=" + ourCode + (pending.fClientState != null ? "&state=" + pending.fClientState : "") );
				}
				else
				{
					final PrintWriter pw = ctx.response ().getStreamForTextResponse ();
					pw.println ( "<html><body><h2>Authorization complete</h2><p>You may close this window and return to your application.</p></body></html>" );
					pw.flush ();
				}
			}
			else
			{
				fPendingAuths.remove ( state );
				sendStatusCodeAndMessage ( ctx, httpStatus, "Token exchange: " + responseBody );
			}
		}
		catch ( URISyntaxException | IOException x )
		{
			fPendingAuths.remove ( state );
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k500_internalServerError, x.getMessage () );
		}
	}

	/**
	 * handle for GET /token?state=... — polled by the MCP client after opening the auth URL.
	 * Returns 202 while the user is still in the browser auth flow, 200 + token JSON when complete.
	 * @param ctx
	 */
	public void token ( CHttpRequestContext ctx )
	{
		if ( !fInfo.enableAuthFlowEndpoints () )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k404_notFound, "not found" );
			return;
		}

		fPendingAuths.entrySet ().removeIf ( e -> e.getValue ().isExpired () );

		final String state = ctx.request ().getParameter ( "state", null );
		if ( state == null )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k400_badRequest, "missing state" );
			return;
		}

		final PendingAuth pending = fPendingAuths.get ( state );
		if ( pending == null )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k404_notFound, "unknown or expired state" );
			return;
		}

		if ( pending.isPending () )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k202_accepted, "authorization pending" );
			return;
		}

		fPendingAuths.remove ( state );
		sendJson ( ctx, pending.fToken );
	}

	/**
	 * handle for POST /token — standard OAuth code exchange
	 */
	public void postToken ( CHttpRequestContext ctx )
	{
		if ( !fInfo.enableAuthFlowEndpoints () )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k404_notFound, "not found" );
			return;
		}

		try
		{
			String code = ctx.request().getParameter("code", null);
			if ( code == null )
			{
				try
				{
					final JSONObject json = readBody(ctx);
					if (json != null) code = json.optString("code", null);
				}
				catch (Exception x) { /* ignore */ }
			}

			if ( code == null )
			{
				sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k400_badRequest, "missing code" );
				return;
			}

			final PendingAuth pending = fCodeToPending.remove ( code );
			if ( pending == null || pending.isExpired () )
			{
				sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k400_badRequest, "invalid or expired code" );
				return;
			}

			sendJson ( ctx, pending.fToken );
		}
		catch ( Exception x )
		{
			sendStatusCodeAndMessage ( ctx, HttpStatusCodes.k500_internalServerError, x.getMessage () );
		}
	}

	/**
	 * handle for GET /mcp — opens an SSE stream for server-initiated messages per the
	 * MCP streamable HTTP transport spec. The connection is kept open; the server writes
	 * events as they arise. Currently sends an endpoint event on connect so the client
	 * knows where to POST subsequent requests.
	 * @param context
	 */
	public void getMcp ( CHttpRequestContext context )
	{
		if ( fInfo.enableAuthFlowEndpoints () )
		{
			final String authHeader = context.request ().getFirstHeader ( "Authorization" );
			if ( authHeader == null || !authHeader.startsWith ( "Bearer " ) )
			{
				context.response ().writeHeader ( "WWW-Authenticate", "Bearer" );
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, "Unauthorized" );
				return;
			}
		}

		try
		{
			// Securely resume session using standard SSE Last-Event-ID
			final String requestedId = context.request ().getFirstHeader ( "Last-Event-ID" );
			Session session = requestedId != null ? fSessions.get ( requestedId ) : null;
			if ( session == null )
			{
				session = fSessions.create ();
			}
			final String sessionId = session.getId ();

			final PrintWriter pw = context.response ().getStreamForTextResponse ( "text/event-stream" );
			final String postUrl = derivePostMcpUrl ( context, sessionId );

			// Send the session ID as the SSE event ID so the client sends it back
			// in the Last-Event-ID header upon reconnect
			pw.print ( "id: " );
			pw.print ( sessionId );
			pw.print ( "\nevent: endpoint\ndata: " );
			pw.print ( postUrl );
			pw.print ( "\n\n" );
			pw.flush ();

			try
			{
				while ( true )
				{
					final JSONObject msg = session.poll ( 5, TimeUnit.SECONDS );
					if ( msg != null )
					{
						pw.print ( "event: message\ndata: " );
						pw.print ( msg.toString () );
						pw.print ( "\n\n" );
						pw.flush ();

						log.info ( "MCP wrote [{}]", msg );
					}
					if ( pw.checkError () ) break;
				}
			}
			catch ( InterruptedException e )
			{
				Thread.currentThread().interrupt();
			}
			finally
			{
				// We no longer remove the session immediately on disconnect so the
				// client can reconnect and resume the session via ?sessionId=...
				// Note: You should consider adding a TTL/cleanup job to McpSessionStore
				// to eventually evict inactive sessions.
				// fSessions.remove ( sessionId );
			}
		}
		catch ( URISyntaxException | IOException x )
		{
			sendStatusCodeAndMessage ( context, HttpStatusCodes.k500_internalServerError, x.getMessage () );
		}
	}

	/**
	 * handle for POST /mcp
	 * @param context
	 */
	public void postMcp ( CHttpRequestContext context, String sessionId )
	{
		postMcpInternal ( context, sessionId );
	}

	/**
	 * handle for POST /mcp (stateless HTTP transport)
	 */
	public void postMcpStateless ( CHttpRequestContext context )
	{
		postMcpInternal ( context, null );
	}

	private void postMcpInternal ( CHttpRequestContext context, String sessionId )
	{
		String token = null;
		if ( fInfo.enableAuthFlowEndpoints () )
		{
			final String authHeader = context.request ().getFirstHeader ( "Authorization" );
			if ( authHeader == null || !authHeader.startsWith ( "Bearer " ) )
			{
				context.response ().writeHeader ( "WWW-Authenticate", "Bearer" );
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, "Unauthorized" );
				return;
			}
			token = authHeader.substring ( 7 ).trim ();
		}

		Session session = null;
		if ( sessionId != null && !sessionId.isBlank () )
		{
			session = fSessions.get ( sessionId );
			if ( session == null )
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "Unknown sessionId" );
				return;
			}
		}

		try
		{
			final JSONObject body = readBody ( context );
			log.info ( "MCP recd POST [{}]", body.toString () );

			final String id = body.opt ( "id" ) != null ? body.get ( "id" ).toString () : null;
			final String method = body.optString ( "method", "" );
			final JSONObject params = body.optJSONObject ( "params" );

			JSONObject responseObj = null;

			switch ( method )
			{
				case "notifications/initialized":
					break;

				case "initialize":
				{
					final JSONObject result = new JSONObject ()
						.put ( "protocolVersion", "2024-11-05" )
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
					if ( fInfo.hasAuthInfo () )
					{
						final JSONObject auth = new JSONObject ()
							.put ( "domain", fInfo.getAuthDomain () )
							.put ( "clientId", fInfo.getAuthClientId () )
						;
						if ( fInfo.getAuthAudience () != null )
						{
							auth.put ( "audience", fInfo.getAuthAudience () );
						}
						result.put ( "auth", auth );
					}
					responseObj = jsonRpcResult ( id, result );
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
					responseObj = jsonRpcResult ( id, new JSONObject ().put ( "tools", tools ) );
					break;
				}

				case "tools/call":
				{
					final String toolName = params != null ? params.optString ( "name", null ) : null;
					if ( toolName == null )
					{
						responseObj = jsonRpcError ( id, kJsonRpcErr_InvalidParams, "missing tool name" );
						break;
					}
					final McpTool tool = fTools.getTool ( toolName );
					if ( tool == null )
					{
						responseObj = jsonRpcError ( id, kJsonRpcErr_InvalidParams, "unknown tool: " + toolName );
						break;
					}
					final JSONObject args = params.optJSONObject ( "arguments" );
					try
					{
//						final UserContext<I> user = mcpGetUser ( session.getToken () );
						final List<ResponseBlock> response = tool.call ( token, args != null ? args : new JSONObject () );

						responseObj = jsonRpcResult ( id, new JSONObject ()
							.put ( "content", JsonVisitor.collectionToArray ( response ) )
							.put ( "isError", false )
						);
					}
					catch ( McpTool.McpToolUnauthorizedException x )
					{
						context.response ().writeHeader ( "WWW-Authenticate", "Bearer" );
						sendStatusCodeAndMessage ( context, HttpStatusCodes.k401_unauthorized, "Unauthorized" );
						return;
					}
					catch ( McpTool.McpToolException x )
					{
						final JSONArray content = new JSONArray ()
							.put ( new JSONObject ()
								.put ( "type", "text" )
								.put ( "text", x.getCause () != null ? x.getCause ().getMessage () : x.getMessage () )
							);
						responseObj = jsonRpcResult ( id, new JSONObject ()
							.put ( "content", content )
							.put ( "isError", true )
						);
					}
//					catch ( IamSvcException x )
//					{
//						final JSONArray content = new JSONArray ()
//							.put ( new JSONObject ()
//								.put ( "type", "text" )
//								.put ( "text", "There was a problem with the authentication system." )
//							);
//						responseObj = jsonRpcResult ( id, new JSONObject ()
//							.put ( "content", content )
//							.put ( "isError", true )
//						);
//					}
					break;
				}

				default:
					if ( id != null )
					{
						responseObj = jsonRpcError ( id, kJsonRpcErr_MethodNotFound, "method not found: " + method );
					}
					break;
			}

			if ( responseObj != null && id != null )
			{
				if ( session != null )
				{
					session.offer ( responseObj );
					log.info ( "MCP queued [{}]", responseObj.toString () );
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k202_accepted, "Accepted" );
				}
				else
				{
					sendJson ( context, responseObj );
				}
			}
			else
			{
				if ( session != null )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k202_accepted, "Accepted" );
				}
				else
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k200_ok, "OK" );
				}
			}
		}
		catch ( JSONException | IOException x )
		{
			final JSONObject err = jsonRpcError ( null, kJsonRpcErr_ParseError, "parse error: " + x.getMessage () );
			if ( session != null )
			{
				session.offer ( err );
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k202_accepted, "Accepted" );
			}
			else
			{
				sendJson ( context, err );
			}
		}
	}

//	private UserContext<I> mcpGetUser ( String token ) throws IamSvcException
//	{
//		return getUser ( token );
//	}

	private static String derivePostMcpUrl ( CHttpRequestContext ctx, String sessionId ) throws URISyntaxException
	{
		final URI req = getOriginalUri ( ctx );
		return new URI ( req.getScheme (), req.getAuthority (), "/mcp/" + sessionId, null, null ).toString ();
	}

	private static String deriveCallbackUri ( CHttpRequestContext ctx ) throws URISyntaxException
	{
		final URI req = getOriginalUri ( ctx );
		final String path = req.getPath ();
		final String callbackPath = path.substring ( 0, path.lastIndexOf ( '/' ) + 1 ) + "callback";
		return new URI ( req.getScheme (), req.getAuthority (), callbackPath, null, null ).toString ();
	}

	private static URI getOriginalUri ( CHttpRequestContext ctx ) throws URISyntaxException
	{
		final URI req = new URI ( ctx.request ().getUrl () );
		String scheme = req.getScheme ();
		String authority = req.getAuthority ();

		final String forwardedProto = ctx.request ().getFirstHeader ( "X-Forwarded-Proto" );
		if ( forwardedProto != null && !forwardedProto.trim ().isEmpty () )
		{
			scheme = forwardedProto.split ( "," ) [0].trim ();
		}

		final String forwardedHost = ctx.request ().getFirstHeader ( "X-Forwarded-Host" );
		if ( forwardedHost != null && !forwardedHost.trim ().isEmpty () )
		{
			authority = forwardedHost.split ( "," ) [0].trim ();
		}

		return new URI ( scheme, authority, req.getPath (), req.getQuery (), req.getFragment () );
	}

	private static JSONObject jsonRpcResult ( String id, JSONObject result )
	{
		final JSONObject r = new JSONObject ()
			.put ( "jsonrpc", "2.0" )
			.put ( "result", result );
		if ( id != null ) r.put ( "id", id );
		return r;
	}

	private static JSONObject jsonRpcError ( String id, int code, String message )
	{
		final JSONObject r = new JSONObject ()
			.put ( "jsonrpc", "2.0" )
			.put ( "error", new JSONObject ()
				.put ( "code", code )
				.put ( "message", message )
			);
		if ( id != null ) r.put ( "id", id );
		return r;
	}

	private final McpInfo fInfo;
	private final McpToolRegistry fTools;
	private final ConcurrentHashMap<String,PendingAuth> fPendingAuths = new ConcurrentHashMap<> ();
	private final ConcurrentHashMap<String,PendingAuth> fCodeToPending = new ConcurrentHashMap<> ();
	private final McpSessionStore fSessions;

	private static class PendingAuth
	{
		PendingAuth ( String clientRedirectUri, String clientState )
		{
			fClientRedirectUri = clientRedirectUri;
			fClientState = clientState;
			fCreatedAtMs = Clock.now ();
			fCompletedAtMs = 0L;
			fToken = null;
		}

		void complete ( JSONObject token )
		{
			fToken = token;
			fCompletedAtMs = Clock.now ();
		}

		boolean isPending () { return fToken == null; }

		boolean isExpired ()
		{
			final long now = Clock.now ();
			return isPending ()
				? now - fCreatedAtMs   > kPendingTtlMs
				: now - fCompletedAtMs > kCompletedTtlMs;
		}

		public final String fClientRedirectUri;
		public final String fClientState;
		private final long fCreatedAtMs;
		private long fCompletedAtMs;
		JSONObject fToken;

		private static final long kPendingTtlMs   = 5L * 60 * 1000;  // 5 min to complete browser auth
		private static final long kCompletedTtlMs = 1L * 60 * 1000;  // 1 min for client to pick up token
	}

	private static final Logger log = LoggerFactory.getLogger ( McpEndpoints.class );
}
