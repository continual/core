package io.continual.services.processor.engine.library.sources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.standards.MimeTypes;

public class InputStreamSource extends BasicSource
{
	public InputStreamSource ( InputStream is )
	{
		this ( is, true );
	}

	public InputStreamSource ( InputStream is, boolean withClose )
	{
		fReader = new BufferedReader ( new InputStreamReader ( is ) );
		fWithClose = withClose;
	}

	@Override
	public synchronized void close () throws IOException
	{
		if ( fWithClose ) 
		{
			fReader.close ();
		}
		super.close ();
	}

	/**
	 * Get the next pending message, if any. This won't be called after a noteEndOfStream() call.
	 * The caller handles back-off, so it's not necessary to force a sleep during this call. 
	 * The object has the instance synchronization lock during this call.
	 * @param spc
	 * @return the next message, or null
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Override
	protected MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc ) throws IOException, InterruptedException
	{
		final String line = fReader.readLine ();
		if ( line == null )
		{
			noteEndOfStream ();
			return null;
		}

		// make a message with the line as plain text
		final JSONObject json = new JSONObject ()
			.put ( MimeTypes.kPlainText, line )
		;

		// attempt to add the line as parsed JSON
		try
		{
			json.put ( MimeTypes.kAppJson, new JSONObject ( new CommentedJsonTokener ( line ) ) );
		}
		catch ( JSONException x )
		{
			// ignore
		}

		// return the message
		return makeDefRoutingMessage ( Message.adoptJsonAsMessage ( json ) );
	}

	private final BufferedReader fReader;
	private final boolean fWithClose;
}
