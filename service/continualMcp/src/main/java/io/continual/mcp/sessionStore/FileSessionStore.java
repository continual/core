package io.continual.mcp.sessionStore;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.mcp.McpSessionStore;

public class FileSessionStore implements McpSessionStore
{
	public FileSessionStore ( JSONObject config ) throws BuildFailure
	{
		try
		{
			fBaseDir = new File ( config.getString ( "baseDir" ) );
			if ( !fBaseDir.exists () )
			{
				fBaseDir.mkdirs ();
			}
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public Session create ()
	{
		return create ( UUID.randomUUID ().toString () );
	}

	@Override
	public Session create ( String id )
	{
		final File file = getFileFor ( id );
		try
		{
			Files.write ( file.toPath (), new JSONObject ().toString ().getBytes ( StandardCharsets.UTF_8 ) );
		}
		catch ( IOException e )
		{
			log.warn ( "Failed to create session {}: {}", id, e.getMessage () );
		}
		return new FileSession ( id );
	}

	@Override
	public Session get ( String id )
	{
		final File file = getFileFor ( id );
		if ( file.exists () )
		{
			return new FileSession ( id );
		}
		return null;
	}

	@Override
	public void remove ( String id )
	{
		final File file = getFileFor ( id );
		if ( file.exists () )
		{
			file.delete ();
		}
		fMemoryQueues.remove ( id );
	}

	private File getFileFor ( String id )
	{
		return new File ( fBaseDir, id + ".json" );
	}

	private final File fBaseDir;
	private final ConcurrentHashMap<String, BlockingQueue<JSONObject>> fMemoryQueues = new ConcurrentHashMap<> ();

	private class FileSession implements Session
	{
		public FileSession ( String id )
		{
			fId = id;
			fMemoryQueues.putIfAbsent ( id, new LinkedBlockingQueue<> () );
		}

		@Override
		public String getId () { return fId; }

		@Override
		public void offer ( JSONObject msg )
		{
			fMemoryQueues.get ( fId ).add ( msg );
		}

		@Override
		public JSONObject poll ( int i, TimeUnit tu ) throws InterruptedException
		{
			return fMemoryQueues.get ( fId ).poll ( i, tu );
		}

		private final String fId;
	}

	private static final Logger log = LoggerFactory.getLogger ( FileSessionStore.class );
}
