package io.continual.messaging;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * A generalized destination for messages, which are optionally sent as part of a specific
 * message stream.
 */
public interface ContinualMessageSink
{
	/**
	 * Send a message to the sink using its general message stream.
	 * @param message
	 */
	default void send ( ContinualMessage msg ) throws MessagePublishException
	{
		send ( kGeneralMsgStream, msg );
	}

	/**
	 * Send a list of messages to the sink using its general message stream.
	 * @param msgs
	 */
	default void send ( ContinualMessage... msgs ) throws MessagePublishException
	{
		send ( kGeneralMsgStream, msgs );
	}

	/**
	 * Send a list of messages to the sink using its general message stream.
	 * @param msgs
	 */
	default void send ( Collection<ContinualMessage> msgs ) throws MessagePublishException
	{
		send ( kGeneralMsgStream, msgs );
	}

	/**
	 * Send a message to the sink using the given message stream.
	 * @param stream
	 * @param message
	 */
	default void send ( ContinualMessageStream stream, ContinualMessage message ) throws MessagePublishException
	{
		send ( stream, Collections.singletonList ( message ) );
	}

	/**
	 * Send a list of messages to the sink using the given message stream.
	 * @param stream
	 * @param msgs
	 */
	default void send ( ContinualMessageStream stream, ContinualMessage... msgs ) throws MessagePublishException
	{
		send ( stream, Arrays.asList ( msgs ) );
	}

	/**
	 * Send a collection (iterated in order) of messages to the sink using the given message stream.
	 * @param stream
	 * @param msgs
	 */
	void send ( ContinualMessageStream stream, Collection<ContinualMessage> msgs ) throws MessagePublishException;

	/**
	 * the conventional general message stream name
	 */
	public static final String kGeneralMessageStreamName = ".";

	/**
	 * a standard message stream from the conventional general message stream name
	 */
	public static final ContinualMessageStream kGeneralMsgStream = ContinualMessageStream.fromName ( kGeneralMessageStreamName );
}
