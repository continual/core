package io.continual.services.processor.engine.library.sinks;

import java.io.IOException;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.jsonHttpClient.HttpUsernamePasswordCredentials;
import io.continual.jsonHttpClient.JsonOverHttpClient;
import io.continual.jsonHttpClient.JsonOverHttpClient.BodyFormatException;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpRequest;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpResponse;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpServiceException;
import io.continual.jsonHttpClient.impl.ok.OkHttp;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Sink;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.time.Clock;

public class RcvrSink implements Sink
{
	public interface BackoffAlgo
	{
		long getNextWait ( int attemptNumber, long lastValue );
	}
	
	public static class Builder
	{
		public Builder () {}

		public Builder sendingTo ( String host )
		{
			if ( host != null && host.length () > 0 )
			{
				if ( !host.contains ( "://" ) )
				{
					host = "http://" + host;
				}
				fHost = host;
			}
			return this;
		}

		public Builder onTopic ( String topic )
		{
			fTopic = topic;
			return this;
		}

		public Builder onStream ( String stream )
		{
			fStream = stream;
			return this;
		}

		public Builder asUser ( String user, String pwd )
		{
			if ( user != null && user.length () > 0 )
			{
				fCreds = new HttpUsernamePasswordCredentials ( user, pwd );
			}
			else
			{
				fCreds = null;
			}
			return this;
		}
		
		public Builder maxCacheLength ( int cacheLength )
		{
			fMaxCacheLength = Math.max ( 0, cacheLength );
			return this;
		}

		public Builder maxCacheAge ( long cacheAgeMs )
		{
			fMaxCacheAgeMs = Math.max ( 0L, cacheAgeMs );
			return this;
		}

		public Builder backingOff ( BackoffAlgo ba )
		{
			if ( ba != null )
			{
				fBackoffAlgo = ba;
			}
			return this;
		}

		public RcvrSink build () throws BuildFailure
		{
			if ( fStream != null && fTopic == null )
			{
				throw new BuildFailure ( "You may not set a stream without setting a topic." );
			}
			return new RcvrSink ( this );
		}

		private String fHost = "https://rcvr.continual.io";
		private String fTopic = null;
		private String fStream = null;
		private HttpUsernamePasswordCredentials fCreds = null;
		private int fMaxCacheLength = 0;
		private long fMaxCacheAgeMs = 0L;
		private BackoffAlgo fBackoffAlgo = new BackoffAlgo ()
		{
			@Override
			public long getNextWait ( int attemptNumber, long lastValue ) { return 5 * 1000L; }
		};
	}

	@Override
	public void init ()
	{
		// nothing to do
	}

	@Override
	public void close () throws IOException
	{
		// nothing to do
	}

	@Override
	public void flush ()
	{
		flush ( null );
	}

	@Override
	public synchronized void process ( MessageProcessingContext context )
	{
		StringBuilder path = new StringBuilder ()
			.append ( "/events" )
		;
		if ( fTopic != null )
		{
			final String topic = context.evalExpression ( fTopic );
			if ( topic != null && topic.length () > 0 )
			{
				path.append ( "/" ).append ( topic );
				if ( fStream != null )
				{
					final String stream = context.evalExpression ( fStream );
					if ( stream != null && stream.length () > 0 )
					{
						path.append ( "/" ).append ( stream );
						
					}
				}
			}
		}

		final String thisPath = path.toString ();
		final JSONObject msg = context.getMessage ().toJson ();
		if ( fPendingSends.size () > 0 && fPendingSends.peekFirst ().pathIsNot ( thisPath ) )
		{
			flush ( context.getStreamProcessingContext () );
		}

		fPendingSends.add ( new Packet ( thisPath, msg ) );
		if (
			fPendingSends.size () > fMaxCacheLength ||
			fPendingSends.peekFirst ().isOlderThan ( fMaxCacheAgeMs )
		)
		{
			flush ( context.getStreamProcessingContext () );
		}
	}

	private RcvrSink ( Builder b )
	{
		fHost = b.fHost;
		fTopic = b.fTopic;
		fStream = b.fStream;
		fCreds = b.fCreds;
		fMaxCacheLength = b.fMaxCacheLength;
		fMaxCacheAgeMs = b.fMaxCacheAgeMs;
		fBackoffAlgo = b.fBackoffAlgo;

		fClient = new OkHttp ();
		fPendingSends = new LinkedList<> ();

		// FIXME: need to establish a timer for pending msgs that are too old without a triggering event behind them
	}

	private static class Packet
	{
		public Packet ( String path, JSONObject msg )
		{
			fPath = path;
			fMsg = msg;
			fQueueTimeMs = Clock.now ();
		}

		public boolean pathIsNot ( String otherPath )
		{
			return !fPath.equals ( otherPath );
		}

		public boolean isOlderThan ( long durationMs )
		{
			final long ageMs = Clock.now () - fQueueTimeMs;
			return ageMs > durationMs;
		}

		public final String fPath;
		public final JSONObject fMsg;
		public final long fQueueTimeMs;
	}

	private void warn ( StreamProcessingContext spc, String msg )
	{
		if ( spc != null )
		{
			spc.warn ( msg );
		}
		else
		{
			log.warn ( msg );
		}
	}

	private synchronized void flush ( StreamProcessingContext spc )
	{
		if ( fPendingSends.size () == 0 ) return;

		final String path = fHost + fPendingSends.peekFirst ().fPath;

		final JSONArray body = new JSONArray ();
		for ( Packet p : fPendingSends )
		{
			body.put ( p.fMsg );
		}
		fPendingSends.clear ();

		try
		{
			boolean sentOk = false;
			int thisAttempt = 1;
			final int maxAttempts = 3;
			long waitMs = 0L;
			boolean badRequest = false;
			while ( !sentOk && !badRequest && thisAttempt <= maxAttempts )
			{
				if ( thisAttempt++ > 1 )
				{
					waitMs = fBackoffAlgo.getNextWait ( thisAttempt, waitMs );
					if ( waitMs > 0L )
					{
						Thread.sleep ( waitMs );
					}
				}

				HttpRequest req = fClient.newRequest ()
					.onPath ( path )
				;
				if ( fCreds != null )
				{
					req.asUser ( fCreds );
				}

				try (
					final HttpResponse response = req
						.post ( body )
				)
				{
					sentOk = response.isSuccess ();
					if ( !sentOk )
					{
						warn ( spc, "Error posting to " + path + ": " + response.getCode () + " " + response.getMessage () + "; " + response.getBody ().toString () );
						badRequest = response.isClientError ();
					}
				}
				catch ( HttpServiceException e )
				{
					warn ( spc, "Error posting to " + path + ": " + e.getMessage () );
				}
				catch ( BodyFormatException x )
				{
					warn ( spc, "Response format was flawed from " + path + ": " + x.getMessage () );
				}
			}

			if ( !sentOk )
			{
				if ( badRequest )
				{
					warn ( spc, "Post of " + body.length () + " messages resulted in bad request. MESSAGES DROPPED." );
				}
				else
				{
					warn ( spc, "Failed to post " + body.length () + " messages after " + (thisAttempt-1) + " attempts. MESSAGES DROPPED." );
				}
			}
		}
		catch ( InterruptedException x )
		{
			warn ( spc, "Interrupted while waiting to send " + body.length () + " messages. MESSAGES DROPPED." );
		}
	}

	private final String fHost;
	private final String fTopic;
	private final String fStream;
	private final HttpUsernamePasswordCredentials fCreds;
	private final int fMaxCacheLength;
	private final long fMaxCacheAgeMs;
	private final BackoffAlgo fBackoffAlgo;

	private final JsonOverHttpClient fClient;
	private final LinkedList<Packet> fPendingSends;

	private static final Logger log = LoggerFactory.getLogger ( RcvrSink.class );
}
