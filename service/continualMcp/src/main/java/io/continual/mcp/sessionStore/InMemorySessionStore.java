package io.continual.mcp.sessionStore;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.continual.mcp.McpSessionStore;

public class InMemorySessionStore implements McpSessionStore
{
	public InMemorySessionStore ()
	{
	}

	@Override
	public Session create ()
	{
		return create ( UUID.randomUUID ().toString () );
	}

	@Override
	public Session create ( String id )
	{
		final InMemorySession session = new InMemorySession ( id );
		fSessions.put ( id, session );
		return session;
	}

	@Override
	public Session get ( String id )
	{
		return fSessions.get ( id );
	}

	@Override
	public void remove ( String id )
	{
		fSessions.remove ( id );
	}

	private final ConcurrentHashMap<String,InMemorySession> fSessions = new ConcurrentHashMap<> ();

	private class InMemorySession implements Session
	{
		public InMemorySession ( String id )
		{
			fId = id;
			fQueue = new LinkedBlockingQueue<> ();
		}

		@Override
		public String getId () { return fId; }

		@Override
		public void offer ( JSONObject msg )
		{
			fQueue.add ( msg );
		}

		@Override
		public JSONObject poll ( int i, TimeUnit tu ) throws InterruptedException
		{
			return fQueue.poll ( i, tu );
		}

		private final String fId;
		private final BlockingQueue<JSONObject> fQueue;
	}
}
