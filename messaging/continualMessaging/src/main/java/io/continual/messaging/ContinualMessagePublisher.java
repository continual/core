
package io.continual.messaging;

import java.io.Closeable;

/**
 * An abstraction of a message publisher for this system. A publisher contains message sinks.
 */
public interface ContinualMessagePublisher
	extends Closeable
{
	/**
	 * Topic unavailable exception is thrown when the topic cannot be used at this time.
	 */
	public static class TopicUnavailableException
		extends MessagePublishException
	{
		public TopicUnavailableException ()
		{
			super ();
		}

		public TopicUnavailableException ( Throwable t )
		{
			super ( t );
		}

		public TopicUnavailableException ( String msg )
		{
			super ( msg );
		}

		public TopicUnavailableException ( String msg, Throwable t )
		{
			super ( msg, t );
		}

		private static final long serialVersionUID = 1L;
	}

	/**
	 * Get a message sink for a specific topic.
	 *
	 * @param topic
	 * @return a message sink
	 * @throws TopicUnavailableException
	 *             if the topic name is not supported or somehow not available
	 */
	ContinualMessageSink getTopic ( String topic )
		throws TopicUnavailableException;

	/**
	 * For implementations with batching, flush the pending messages.
	 */
	void flush ()
		throws MessagePublishException;
}
