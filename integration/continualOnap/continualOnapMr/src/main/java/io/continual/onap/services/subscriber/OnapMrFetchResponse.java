package io.continual.onap.services.subscriber;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.continual.onap.services.mrCommon.OnapMrResponse;

public class OnapMrFetchResponse extends OnapMrResponse
{
	/**
	 * Construct this response with an empty set of messages and no EOF.
	 * @param statusCode the HTTP status code
	 * @param statusMsg the HTTP status text
	 */
	public OnapMrFetchResponse ( int statusCode, String statusMsg )
	{
		super ( statusCode, statusMsg );
		fMsgs = new LinkedList<String> ();
		fEof = false;
	}

	/**
	 * Construct this response with a finished set of messages. 
	 * @param statusCode the HTTP status code
	 * @param statusMsg the HTTP status text
	 * @param msgs the set of messages in the response
	 */
	public OnapMrFetchResponse ( int statusCode, String statusMsg, List<String> msgs )
	{
		this ( statusCode, statusMsg );
		fMsgs.addAll ( msgs );
		fEof = true;
	}

	/**
	 * Push a message into this response
	 * @param msg a message from the service
	 * @return this response
	 */
	public synchronized OnapMrFetchResponse push ( String msg )
	{
		if ( fEof ) throw new IllegalStateException ( "Cannot add messages to a closed response." );

		fMsgs.add ( msg );
		notifyAll ();

		return this;
	}

	/**
	 * Mark the response stream as being at its end
	 * @return this response
	 */
	public synchronized OnapMrFetchResponse markEof ()
	{
		fEof = true;
		notifyAll ();

		return this;
	}

	/**
	 * Is this response at the end of its stream, with all pending messages consumed?
	 * @return true if there are no more messages
	 */
	public synchronized boolean isEof ()
	{
		return fMsgs.size () == 0 && fEof;
	}

	/**
	 * Get the number of pending messages in the response. This count does not include
	 * messages that have not yet been delivered via the response stream.
	 * @return the count of pending messages
	 */
	public synchronized int readyCount ()
	{
		return fMsgs.size ();
	}

	/**
	 * Fetch the next message in this response.
	 * @param timeoutMs the length of time to wait for the next message (which could potentially be in-flight)
	 * @return a message, or null if all of the response has been consumed
	 * @throws InterruptedException if the wait is interrupted
	 */
	public synchronized String consumeNext ( long timeoutMs ) throws InterruptedException
	{
		// check EOF (no msgs, and none coming)
		if ( isEof() ) return null; 

		// if we have waiting msgs, return the first
		if ( fMsgs.size () > 0 )
		{
			return fMsgs.remove ();
		}

		// otherwise, wait for more msgs, or the EOF flag
		wait ( timeoutMs );

		if ( fMsgs.size () > 0 )
		{
			return fMsgs.remove ();
		}
		return null;
	}

	/**
	 * Consume all messages in the response, blocking until the end of the stream.
	 * @deprecated Client code should call consumeNext() with a timeout value set
	 * @return a list of messages
	 */
	@Deprecated
	public List<String> getMessages ()
	{
		final ArrayList<String> result = new ArrayList<> ();
		try
		{
			while ( !isEof () )
			{
				final String msg = consumeNext ( 500 );
				if ( msg != null )
				{
					result.add ( msg );
				}
			}
		}
		catch ( InterruptedException e )
		{
			// can't do much about this here...
		}
		return result;
	}

	private final LinkedList<String> fMsgs;
	private boolean fEof;
}
