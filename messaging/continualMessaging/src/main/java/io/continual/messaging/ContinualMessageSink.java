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
	 * @param msg a message
	 * @throws MessagePublishException when a message cannot be published
	 */
	default void send ( ContinualMessage msg ) throws MessagePublishException
	{
		send ( kGeneralMsgStream, msg );
	}

	/**
	 * Send a list of messages to the sink using its general message stream.
	 * @param msgs messages
	 * @throws MessagePublishException when a message cannot be published
	 */
	default void send ( ContinualMessage... msgs ) throws MessagePublishException
	{
		send ( kGeneralMsgStream, msgs );
	}

	/**
	 * Send a list of messages to the sink using its general message stream.
	 * @param msgs messages
	 * @throws MessagePublishException when a message cannot be published
	 */
	default void send ( Collection<ContinualMessage> msgs ) throws MessagePublishException
	{
		send ( kGeneralMsgStream, msgs );
	}

	/**
	 * Send a message to the sink using the given message stream.
	 * @param stream a stream
	 * @param message a message
	 * @throws MessagePublishException when a message cannot be published
	 */
	default void send ( ContinualMessageStream stream, ContinualMessage message ) throws MessagePublishException
	{
		send ( stream, Collections.singletonList ( message ) );
	}

	/**
	 * Send a list of messages to the sink using the given message stream.
	 * @param stream a stream
	 * @param msgs messages
	 * @throws MessagePublishException when a message cannot be published
	 */
	default void send ( ContinualMessageStream stream, ContinualMessage... msgs ) throws MessagePublishException
	{
		send ( stream, Arrays.asList ( msgs ) );
	}

	/**
	 * Send a collection (iterated in order) of messages to the sink using the given message stream.
	 * @param stream a stream
	 * @param msgs messsages
	 * @throws MessagePublishException when a message cannot be published
	 */
	void send ( ContinualMessageStream stream, Collection<ContinualMessage> msgs ) throws MessagePublishException;

	/**
	 * The conventional general message stream name. Messages sent in this stream can be processed in any order.
	 */
	public static final String kGeneralMessageStreamName = ".";

	/**
	 * a standard message stream from the conventional general message stream name.
	 */
	public static final ContinualMessageStream kGeneralMsgStream = ContinualMessageStream.fromName ( kGeneralMessageStreamName );
}
