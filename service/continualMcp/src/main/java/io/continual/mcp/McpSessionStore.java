package io.continual.mcp;

import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

public interface McpSessionStore
{
	interface Session
	{
		String getId ();

		void offer ( JSONObject msg );
		JSONObject poll ( int i, TimeUnit tu ) throws InterruptedException;
	}

	Session get ( String id );

	Session create ();
	Session create ( String id );

	void remove ( String id );
}
