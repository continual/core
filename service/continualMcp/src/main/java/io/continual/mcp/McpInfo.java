package io.continual.mcp;

public class McpInfo
{
	public McpInfo ()
	{
		fInstructions = "";

		fAuthEndpoints = false;
		fAuthDomain = null;
		fAuthClientId = null;
		fAuthClientSecret = null;
		fAuthAudience = null;
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

	public McpInfo setAuthDomain ( String domain )
	{
		fAuthDomain = domain;
		return this;
	}

	public McpInfo setAuthClientId ( String clientId )
	{
		fAuthClientId = clientId;
		return this;
	}

	public McpInfo setAuthClientSecret ( String clientSecret )
	{
		fAuthClientSecret = clientSecret;
		return this;
	}

	public McpInfo setAuthAudience ( String audience )
	{
		fAuthAudience = audience;
		return this;
	}

	/** @return true if enough auth info is present to emit an auth block */
	public boolean hasAuthInfo ()
	{
		return fAuthDomain != null && fAuthClientId != null;
	}

	public String getAuthDomain () { return fAuthDomain; }
	public String getAuthClientId () { return fAuthClientId; }
	public String getAuthClientSecret () { return fAuthClientSecret; }
	public String getAuthAudience () { return fAuthAudience; }

	private String fInstructions;

	private String fName;
	private String fVersion;
	
	private boolean fAuthEndpoints;
	private String fAuthDomain;
	private String fAuthClientId;
	private String fAuthClientSecret;
	private String fAuthAudience;
}
