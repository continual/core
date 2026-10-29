package io.continual.mcp;

public class McpInfo
{
	public McpInfo ()
	{
		fInstructions = "";
		fAuthEndpoints = false;
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

	public String getAuthServer () { return fAuthServer; }

	private String fInstructions;

	private String fName;
	private String fVersion;
	
	private boolean fAuthEndpoints;
	private String fAuthServer;
}
