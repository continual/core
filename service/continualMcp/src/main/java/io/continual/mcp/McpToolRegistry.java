package io.continual.mcp;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * A registry of {@link McpTool} instances available to an {@link McpService}.
 * Tools are keyed by name; registering a second tool with the same name replaces the first.
 */
public class McpToolRegistry
{
	public McpToolRegistry ()
	{
		fTools = new LinkedHashMap<> ();
	}

	/**
	 * Register a tool. If a tool with the same name is already registered it is replaced.
	 * @param tool the tool to register
	 * @return this registry, for chaining
	 */
	public McpToolRegistry registerTool ( McpTool tool )
	{
		fTools.put ( tool.getName (), tool );
		return this;
	}

	/**
	 * Return all registered tools.
	 * @return an unmodifiable view of the registered tools
	 */
	public Collection<McpTool> getTools ()
	{
		return fTools.values ();
	}

	/**
	 * Look up a tool by name.
	 * @param name the tool name as returned by {@link McpTool#getName()}
	 * @return the tool, or {@code null} if no tool with that name is registered
	 */
	public McpTool getTool ( String name )
	{
		return fTools.get ( name );
	}

	private final LinkedHashMap<String,McpTool> fTools;
}
