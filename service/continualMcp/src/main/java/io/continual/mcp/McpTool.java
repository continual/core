package io.continual.mcp;

import java.util.List;

import org.json.JSONObject;

import io.continual.util.data.json.JsonSerialized;

/**
 * An interface for an MCP tool
 */
public interface McpTool
{
	class McpToolException extends Exception
	{
		public McpToolException ( Exception x ) { super(x); }
		private static final long serialVersionUID = 1L;
	};

	class McpToolUnauthorizedException extends McpToolException
	{
		public McpToolUnauthorizedException ( Exception x ) { super(x); }
		public McpToolUnauthorizedException ( String msg ) { super(new Exception(msg)); }
		private static final long serialVersionUID = 1L;
	};

	/** The tool name as it appears in tools/list and tools/call. */
	String getName ();

	/** Human-readable description sent to the LLM. */
	String getDescription ();

	/**
	 * A response block
	 */
	interface ResponseBlock extends JsonSerialized
	{
	};

	/**
	 * Create a text block response
	 * @param text
	 * @return a text response block
	 */
	static ResponseBlock textBlock ( String text )
	{
		return new ResponseBlock ()
		{
			@Override
			public JSONObject toJson ()
			{
				return new JSONObject ()
					.put ( "type", "text" )
					.put ( "text", text )
				;
			}		
		};
	}

	/**
	 * Create a json block response
	 * @param json
	 * @return a response block
	 */
	static ResponseBlock jsonBlock ( JSONObject json )
	{
		return textBlock ( json.toString () );
	}

	/**
	 * Execute the tool with the given arguments and return a result.
	 * Throw any exception to signal a tool-level error — it will be returned
	 * as an isError:true content block rather than a JSON-RPC protocol error.
	 * 
	 * @param authToken the user's auth token (or null if not enabled)
	 * @param arguments a JSON object of arguments to the tool
	 */
	List<ResponseBlock> call ( String authToken, JSONObject arguments ) throws McpToolException;

	/**
	 * JSON Schema object describing the tool's input parameters, e.g.:
	 * <pre>
	 * {
	 *   "type": "object",
	 *   "properties": { "text": { "type": "string" } },
	 *   "required": ["text"]
	 * }
	 * </pre>
	 * Note that this is required from all tools, even if it's empty
	 */
	default JSONObject getInputSchema ()
	{
		return new JSONObject ()
			.put ( "type", "object" )
			.put ( "properties", new JSONObject () )
		;
	}
}
