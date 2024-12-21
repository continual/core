package io.continual.services.processor.engine.library.sources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.data.json.CommentedJsonTokener;

public class FileWatch extends BasicSource
{
	public FileWatch ( final ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		super ( config );

		fFilename = config.getString ( "file" );
		boolean startWithRead = config.optBoolean ( "startWithRead", false );
		fLastChangeTimeMs = startWithRead ? 0L : getFileTimestamp ( null );
	}

	@Override
	protected MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc )
	{
		final long currentChangeTimeMs = getFileTimestamp ( spc );
		if ( currentChangeTimeMs > fLastChangeTimeMs )
		{
			fLastChangeTimeMs = currentChangeTimeMs;
			try
			{
				return makeDefRoutingMessage ( Message.adoptJsonAsMessage ( loadFileData () ) );
			}
			catch ( IOException x )
			{
				spc.warn ( x.getMessage () );
			}
		}
		return null;
	}

	private final String fFilename;
	private long fLastChangeTimeMs;

	private long getFileTimestamp ( StreamProcessingContext spc )
	{
		final Path path = Paths.get ( fFilename );
		try
		{
			return Files.readAttributes ( path, BasicFileAttributes.class )
				.lastModifiedTime ()
				.toMillis ()
			;
		}
		catch ( IOException e )
		{
			if ( spc != null ) spc.warn ( e.getMessage () );
			return -1;
		}
	}

	private JSONObject loadFileData () throws IOException
	{
		final Path path = Paths.get ( fFilename );

		final byte[] bytes = Files.readAllBytes ( path );
		try ( final ByteArrayInputStream bais = new ByteArrayInputStream ( bytes ) )
		{
			return new JSONObject ( new CommentedJsonTokener ( bais ) );
		}
		catch ( JSONException e )
		{
			return new JSONObject ()
				.put ( "content", new String ( bytes ) )
			;
		}
	}
}
