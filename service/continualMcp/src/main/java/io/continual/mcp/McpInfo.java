package io.continual.mcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class McpInfo
{
	public McpInfo ()
	{
		fInstructions = "";

		fAuthEndpoints = false;

		fAuthScopes = new ArrayList<> ();
		fAuthScopes.add ( "openid" );
		fAuthScopes.add ( "profile" );
		fAuthScopes.add ( "email" );
	}

	public String getName ()
	{
		return fName;
	}

	public McpInfo setName ( String name )
	{
		fName = name;
		return this;
	}

	public String getVersion ()
	{
		return fVersion;
	}

	public McpInfo setVersion ( String version )
	{
		fVersion = version;
		return this;
	}

	public String getInstructions ()
	{
		return fInstructions;
	}

	public McpInfo setInstructions ( String instructions )
	{
		fInstructions = instructions;
		return this;
	}

	public boolean enableAuthFlowEndpoints ()
	{
		return fAuthEndpoints;
	}

	public McpInfo enableAuthFlowEndpoints ( boolean enable )
	{
		fAuthEndpoints = enable;
		return this;
	}

	public McpInfo setAuthServer ( String server )
	{
		fAuthServer = server;
		return this;
	}

	/** @return true if enough auth info is present to emit an auth block */
	public boolean hasAuthInfo ()
	{
		return fAuthServer != null;
	}

	/**
	 * Get the auth server string
	 * @return an auth server string if set
	 */
	public String getAuthServer ()
	{
		return fAuthServer;
	}

	/**
	 * empty auth scopes
	 * @return this MCP info
	 */
	public McpInfo clearAuthScopes ()
	{
		fAuthScopes.clear ();
		return this;
	}
	
	/**
	 * use given auth scopes
	 * @param scopes
	 * @return this MCP info
	 */
	public McpInfo replaceAuthScopes ( List<String> scopes )
	{
		fAuthScopes.clear ();
		if ( scopes != null )
		{
			fAuthScopes.addAll ( scopes );
		}
		return this;
	}

	/**
	 * use the given auth scope
	 * @param scope
	 * @return this MCP info
	 */
	public McpInfo addAuthScope ( String scope )
	{
		fAuthScopes.add ( scope );
		return this;
	}

	/**
	 * Get the configured auth scopes
	 * @return a list of 0 or more auth scopes
	 */
	public List<String> getAuthScopes ()
	{
		return Collections.unmodifiableList ( fAuthScopes );
	}

	private String fInstructions;

	private String fName;
	private String fVersion;
	
	private boolean fAuthEndpoints;
	private String fAuthServer;
	private List<String> fAuthScopes;
}
