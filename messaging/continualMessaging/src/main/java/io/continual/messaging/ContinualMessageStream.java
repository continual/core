package io.continual.messaging;

import io.continual.util.checks.Strings;

/**
 * A message stream is a stream of messages with a name, allowing processors to understand
 * where message order matters. For example, one might label the stream of fault messages coming
 * from a network element using the unique name of the element. 
 */
public class ContinualMessageStream
{
	/**
	 * Construct a message stream from a string name
	 * @param name
	 * @return a message stream
	 */
	public static ContinualMessageStream fromName ( String name )
	{
		return new ContinualMessageStream ( name );
	}

	/**
	 * Construct a message stream using the given name
	 * @param name
	 */
	public ContinualMessageStream ( String name )
	{
		fMessageStreamName = name;

		Strings.throwIfEmpty ( fMessageStreamName, "A message stream name must be a string with at least one character." );
	}

	/**
	 * Get the name of this message stream
	 * @return
	 */
	public String getName ()
	{
		return fMessageStreamName;
	}
	
	@Override public String toString() { return getName(); }

	@Override
	public int hashCode () { return getName().hashCode(); }

	@Override
	public boolean equals ( Object that )
	{
		if ( this == that ) return true;
		if ( that == null ) return false;
		if ( getClass () != that.getClass () ) return false;
		return fMessageStreamName.equals ( ((ContinualMessageStream)that).fMessageStreamName );
	}

	private final String fMessageStreamName;
}
