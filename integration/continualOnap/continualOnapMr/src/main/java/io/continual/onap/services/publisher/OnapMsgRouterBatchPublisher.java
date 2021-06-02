package io.continual.onap.services.publisher;

import java.io.Closeable;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.onap.services.mrCommon.OnapMrResponse;
import io.continual.onap.services.publisher.OnapMsgRouterPublisher.Message;

/**
 * A batching publisher for ONAP Message Router.
 */
public class OnapMsgRouterBatchPublisher implements Closeable
{
	/**
	 * What to do if the send buffer reaches maximum capacity
	 */
	public enum DropPolicy
	{
		DROP_OLDEST,
		DROP_NEWEST;

		public static DropPolicy fromSettingString ( String val )
		{
			if ( val == null ) return DROP_OLDEST;

			try
			{
				return DropPolicy.valueOf ( val.trim ().toUpperCase () );
			}
			catch ( IllegalArgumentException x )
			{
				return DropPolicy.DROP_OLDEST;
			}
		}
	};

	/**
	 * For convenience, just provide a publisher and batching limits
	 * @param pub an underlying publisher
	 * @param atMost how many messages to batch, at most
	 * @param maxAgeMs how long to wait before send, at most
	 * @return a batch publisher
	 */
	public static OnapMsgRouterBatchPublisher build ( OnapMsgRouterPublisher pub, int atMost, int maxAgeMs )
	{
		return new Builder ()
			.usingPublisher ( pub )
			.batchAtMost ( atMost )
			.batchMaxAgeMs ( maxAgeMs )
			.build ()
		;
	}

	public static class Builder
	{
		/**
		 * Construct a batch publisher builder.
		 */
		public Builder () {}

		/**
		 * Specify the publisher to use for sends.
		 * @param pub a publisher to use for batched sends
		 * @return this
		 */
		public Builder usingPublisher ( OnapMsgRouterPublisher pub )
		{
			fPub = pub;
			return this;
		}

		/**
		 * Specify the log to use. If never called, the default logger, named for this class, is used.
		 * @param log the slf4j logger to use for this library. Do not pass null.
		 * @return this builder
		 */
		public Builder logTo ( Logger log )
		{
			fLog = log;
			return this;
		}

		/**
		 * specify how many messages to hold for a batch
		 * @param atMost the limit on the number of messages to batch before sending
		 * @return this
		 */
		public Builder batchAtMost ( int atMost )
		{
			fMaxBatch = atMost;
			return this;
		}
		
		/**
		 * specify how long to wait for messages to join a batch
		 * @param maxAgeMs the limit, in milliseconds, on how long to wait before sending a batch
		 * @return this
		 */
		public Builder batchMaxAgeMs ( int maxAgeMs )
		{
			fMaxTimeMs = maxAgeMs;
			return this;
		}

		public Builder withMaxPendingCount ( int maxPending, DropPolicy dropPolicy )
		{
			fMaxPendingCount = maxPending;
			fMaxPendingDropPolicy = dropPolicy;
			return this;
		}

		/**
		 * specify how long to wait before retrying when the MR call results in an error
		 * @param retryWaitMs the time, in millis, to wait before retrying a failed batch send
		 * @return this
		 */
		public Builder retryAfterMs ( int retryWaitMs )
		{
			fBackoffMs = retryWaitMs;
			return this;
		}

		/**
		 * Build the batching publisher
		 * @return a batching publisher
		 */
		public OnapMsgRouterBatchPublisher build ()
		{
			return new OnapMsgRouterBatchPublisher ( this );
		}

		private OnapMsgRouterPublisher fPub = null;
		private Logger fLog = defaultLog;
		private int fMaxBatch = kDefaultMaxBatch;
		private long fMaxTimeMs = kDefaultMaxTimeMs;
		private long fBackoffMs = kDefaultBackoffTimeMs;
		private long fMaxPendingCount = kDefaultMaxPendingCount;
		private DropPolicy fMaxPendingDropPolicy = DropPolicy.DROP_OLDEST;
	}

	/**
	 * Start the sending thread
	 */
	public synchronized void start ()
	{
		fService.start ();
	}

	/**
	 * Close this publisher to stop its background sending thread.
	 */
	@Override
	public synchronized void close ()
	{
		try
		{
			fService.signalClose ();
			fService.join ();

			// send whatever we have left
			synchronized ( fPendingMsgs )
			{
				final long giveUpAtMs = fPub.getClock().nowMs() + (60*1000L);
				while ( fPendingMsgs.size () > 0 && fPub.getClock().nowMs() < giveUpAtMs )
				{
					final long waitMs = send ();
					if ( waitMs > 0L )
					{
						Thread.sleep ( waitMs );
					}
				}
				if ( fPendingMsgs.size () > 0 )
				{
					fLog.warn ( "Unable to send {} messages before giving up.", fPendingMsgs.size () );
				}
				else
				{
					fLog.info ( "Batch sender closed with no pending messages." );
				}
			}
		}
		catch ( InterruptedException e )
		{
			fLog.warn ( "Interrupted while closing background send thread: {}", e.getMessage () );
			Thread.currentThread ().interrupt ();
		}
	}

	/**
	 * Queue a message to be sent in a batch.
	 * @param msg a message to send
	 * @return this
	 */
	public OnapMsgRouterBatchPublisher send ( Message msg )
	{
		synchronized ( fPendingMsgs )
		{
			fPendingMsgs.add ( new MessageWrapper ( msg ) );
			fPendingMsgs.notify ();
		}
		return this;
	}
	
	/**
	 * Queue a set of messages to be sent in a batch. Iteration order is preserved in the send.
	 * @param msgs a list of messages to send
	 * @return this
	 */
	public OnapMsgRouterBatchPublisher send ( List<Message> msgs )
	{
		final LinkedList<MessageWrapper> wrappers = new LinkedList<> ();
		for ( Message msg : msgs )
		{
			wrappers.add ( new MessageWrapper ( msg ) );
		}
		synchronized ( fPendingMsgs )
		{
			fPendingMsgs.addAll ( wrappers );
			fPendingMsgs.notify ();
		}
		return this;
	}

	private OnapMsgRouterBatchPublisher ( Builder builder )
	{
		fPub = builder.fPub;
		fLog = builder.fLog;
		fPendingMsgs = new LinkedList<> ();
		fMaxBatch = builder.fMaxBatch;
		fMaxTimeMs = builder.fMaxTimeMs;
		fBackoffMs = builder.fBackoffMs;
		fMaxPendingCount = builder.fMaxPendingCount;
		fDropPolicy = builder.fMaxPendingDropPolicy;

		fService = new SvcThread ();

		if ( fPub == null )
		{
			throw new IllegalArgumentException ( "A publisher must be provided." );
		}
	}

	// check for a send now. If not, return the amount of time before a send check is 
	// appropriate.
	private long checkSend ()
	{
		synchronized ( fPendingMsgs )
		{
			final int pending = fPendingMsgs.size ();
			final boolean sendNow =
				( pending >= fMaxBatch ) ||
				( pending > 0 && fPendingMsgs.peekFirst ().queuedAtMs () + fMaxTimeMs <= fPub.getClock().nowMs () )
			;
	
			long result = kEmptyQueueMaxWaitMs;
			if ( sendNow )
			{
				result = send ();
			}
			else if ( pending > 0 )
			{
				final long queuedAtMs = fPendingMsgs.peekFirst ().queuedAtMs ();
				final long dueAtMs = queuedAtMs + fMaxTimeMs;
				result = Math.max ( 0, dueAtMs - fPub.getClock().nowMs() );
			}
			return result;
		}
	}

	private long send ()
	{
		synchronized ( fPendingMsgs )
		{
			final LinkedList<Message> msgs = new LinkedList<> ();
			for ( MessageWrapper wrap : fPendingMsgs )
			{
				msgs.add ( wrap.message () );
			}
	
			final OnapMrResponse response = fPub.send ( msgs );
			if ( response.isSuccess () )
			{
				// clear our pending set
				fPendingMsgs.clear ();
				return kEmptyQueueMaxWaitMs;
			}
			else
			{
				// back-off
				fLog.warn ( "MR send failed with {} {}. Waiting {} ms for retry.", response.getStatusCode (), response.getStatusText (), fBackoffMs );
				return fBackoffMs;
			}
		}
	}

	private void checkForDrops ()
	{
		// inactive?
		if ( fMaxPendingCount < 0 ) return;

		int removals = 0;
		long earliestTs = Long.MAX_VALUE;
		long latestTs = Long.MIN_VALUE;

		synchronized ( fPendingMsgs )
		{
			while ( fPendingMsgs.size () > fMaxPendingCount )
			{
				long queueTimeMs = 0;
				switch ( fDropPolicy )
				{
					case DROP_NEWEST:
						queueTimeMs = fPendingMsgs.removeLast ().queuedAtMs ();
						break;
	
					case DROP_OLDEST:
						queueTimeMs = fPendingMsgs.removeFirst ().queuedAtMs ();
						break;
				}
	
				removals++;
				earliestTs = Math.min ( earliestTs, queueTimeMs);
				latestTs = Math.max ( latestTs, queueTimeMs );
			}
	
			if ( removals > 0 )
			{
				fLog.warn ( "Dropped {} messages with time range from {} to {}.", removals, earliestTs, latestTs );
			}
		}
	}
	
	private final OnapMsgRouterPublisher fPub;
	private final LinkedList<MessageWrapper> fPendingMsgs;
	private final int fMaxBatch;
	private final long fMaxTimeMs;
	private final long fBackoffMs;
	private final long fMaxPendingCount;
	private final DropPolicy fDropPolicy;
	private final Logger fLog;

	private final SvcThread fService;

	private static final Logger defaultLog = LoggerFactory.getLogger ( OnapMsgRouterBatchPublisher.class );

	private static final long kEmptyQueueMaxWaitMs = 100L;
	private static final int kDefaultMaxBatch = 100;
	private static final long kDefaultMaxTimeMs = 500L;
	private static final long kDefaultBackoffTimeMs = 1000L;
	private static final long kDefaultMaxPendingCount = -1L;

	private class MessageWrapper
	{
		public MessageWrapper ( Message msg )
		{
			fMsg = msg;
			fQueuedAtMs = fPub.getClock ().nowMs ();
		}

		public Message message () { return fMsg; }
		public long queuedAtMs () { return fQueuedAtMs; }
		
		public final Message fMsg;
		public final long fQueuedAtMs;
	}

	private class SvcThread extends Thread
	{
		@Override
		public void run ()
		{
			long nextSendDueMs = kEmptyQueueMaxWaitMs;
			while ( !shouldClose () )
			{
				synchronized ( fPendingMsgs )
				{
					try
					{
						if ( nextSendDueMs > 0 )
						{
							// because wait(0) means no timeout!
							fPendingMsgs.wait ( nextSendDueMs );
						}

						nextSendDueMs = checkSend ();
						checkForDrops ();
					}
					catch ( InterruptedException e )
					{
						fLog.warn ( "Background thread interrupted while waiting for input signal: {}", e.getMessage () );
					}
				}
			}
		}

		public synchronized void signalClose ()
		{
			fClose = true;
		}

		private synchronized boolean shouldClose ()
		{
			return fClose;
		}

		private boolean fClose = false;
	}
}
