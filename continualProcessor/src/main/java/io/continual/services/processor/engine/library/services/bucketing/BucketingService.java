package io.continual.services.processor.engine.library.services.bucketing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.library.sources.JsonObjectStreamSource;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Source;
import io.continual.services.processor.service.SimpleProcessingService;

public class BucketingService extends SimpleProcessingService
{
	public static enum Period
	{
		SECONDS
		{
			protected long getFraction () { return 1000; }
		},
		MINUTES
		{
			protected long getFraction () { return SECONDS.getFraction () * 60; }
		},
		HOURS
		{
			protected long getFraction () { return MINUTES.getFraction () * 60; }
		},
		DAYS
		{
			protected long getFraction () { return HOURS.getFraction () * 24; }
		},
		WEEKS
		{
			protected long getFraction () { return DAYS.getFraction () * 7; }
		},
		MONTHS
		{
			protected long getFraction () { return -1; }
		}
		;

		protected abstract long getFraction ();

		public static List<Long> getTimestampsBetween ( Period p, long startMs, long endMs )
		{
			startMs = getBucketTimestamp ( startMs, p );
			endMs = getBucketTimestamp ( endMs, p );
			
			final ArrayList<Long> result = new ArrayList<> ();
			if ( startMs < endMs )
			{
				if ( p == Period.MONTHS )
				{
					// special handling
					final Calendar cal = Calendar.getInstance ();
					cal.setTimeInMillis ( startMs );
					cal.add ( Calendar.MONTH, 1 );
					while ( cal.getTimeInMillis () < endMs )
					{
						result.add ( cal.getTimeInMillis () );
						cal.add ( Calendar.MONTH, 1 );
					}
				}
				else
				{
					final long size = p.getFraction ();
					for ( long tsExpected = startMs + size; tsExpected < endMs; tsExpected += size )
					{
						result.add ( tsExpected );
					}
				}
			}
			return result;
		}

		public static long getBucketTimestamp ( long ms, Period p )
		{
			if ( p == Period.MONTHS )
			{
				// special handling
				final Calendar cal = Calendar.getInstance ();
				cal.setTimeInMillis ( ms );
				cal.set ( Calendar.DAY_OF_MONTH, 15 );
				cal.set ( Calendar.HOUR_OF_DAY, 12 );
				cal.set ( Calendar.MINUTE, 0 );
				cal.set ( Calendar.SECOND, 0 );
				return cal.getTimeInMillis ();
			}
			else
			{
				// we place buckets on the half-way point
				final long f = p.getFraction ();
				return ( ( ms / f ) * f ) + Math.round ( 0.5 * f );
			}
		}

		public static Period readFrom ( String val )
		{
			if ( val == null ) return null;
			val = val.trim ().toUpperCase ();

			return Period.valueOf ( val );
		}
	};

	public static enum StdDataCombiner
	{
		AVERAGE,
		SUM
	};
	
	public interface MessageBridge
	{
		long getTimestamp ( Message m );
		
		Message cloneWithTime ( long tsBucket, Message m );

		Message merge ( Message origEntry, Message m );

		String getKey ( Message m );
	}

	public static long getBucketTimestamp ( Date time, Period p )
	{
		return Period.getBucketTimestamp ( time.getTime (), p );
	}

	public BucketingService ( ConfigLoadContext sc, JSONObject config )
	{
		fSize = Period.readFrom ( config.optString ( "period", Period.MINUTES.toString () ) );
		fOffsetSeconds = config.optLong ( "bucketTimeOffset", 0L );
		fBridge = new StdMsgBridge ( StdDataCombiner.SUM );	// FIXME
		fSet = new HashMap<>();
		fRptTo = null;
		fRptToName = config.optString ( "reportTo" );
	}

	public BucketingService ( Period bucketSize, JsonObjectStreamSource reportTo )
	{
		this ( bucketSize, StdDataCombiner.SUM, reportTo );
	}

	public BucketingService ( Period bucketSize, MessageBridge bridge, JsonObjectStreamSource reportTo )
	{
		fSize = bucketSize;
		fOffsetSeconds = 0L;
		fBridge = bridge;
		fSet = new HashMap<>();
		fRptTo = reportTo;
		fRptToName = null;
	}

	public BucketingService ( Period bucketSize, StdDataCombiner type, JsonObjectStreamSource reportTo )
	{
		this ( bucketSize, new StdMsgBridge ( type ), reportTo );
	}

	@Override
	protected void onStopRequested ()
	{
		close ();
	}

	public synchronized void close ()
	{
		// flush any pending messages out to our pipeline
		flush ();

		if ( fRptTo != null )
		{
			try
			{
				fRptTo.close ();
			}
			catch ( IOException e )
			{
				log.warn ( "Problem closing bucket service target stream: " + e.getMessage () );
			}
		}
	}

	public synchronized void add ( MessageProcessingContext context )
	{
		if ( fRptTo == null && fRptToName != null )
		{
			final Source src = context.getSource ( fRptToName );
			if ( src instanceof JsonObjectStreamSource )
			{
				fRptTo = (JsonObjectStreamSource) src;
			}
		}

		final Message entry = context.getMessage ();

		final long ts = fBridge.getTimestamp ( entry );
		final long tsBucket = Period.getBucketTimestamp ( ts, fSize ) + (fOffsetSeconds*1000L);
		final Message entryAtBucketTime = fBridge.cloneWithTime ( tsBucket, entry );
		final String entryKey = fBridge.getKey ( entryAtBucketTime );

		HashMap<String,Message> entriesAtTime = fSet.get ( tsBucket );
		if ( entriesAtTime == null )
		{
			// this is a new time bucket, so we can flush anything existing
			flush ();
			
			entriesAtTime = new HashMap<> ();
			entriesAtTime.put ( entryKey, entryAtBucketTime );
			fSet.put ( tsBucket, entriesAtTime );
		}
		else
		{
			Message existing = entriesAtTime.get ( entryKey );
			if ( existing == null )
			{
				existing = entryAtBucketTime;
			}
			else
			{
				existing = fBridge.merge ( existing, entry );
			}
			entriesAtTime.put ( entryKey, existing );
		}
	}

	public synchronized List<Message> getBuckets ()
	{
		final LinkedList<Message> result = new LinkedList<> ();

		final LinkedList<Long> timestamps = new LinkedList<> ( fSet.keySet () );
		Collections.sort ( timestamps );

		for ( Long ts : timestamps )
		{
			final HashMap<String,Message> entriesAtTime = fSet.get ( ts );

			final LinkedList<String> keys = new LinkedList<> ( entriesAtTime.keySet () );
			Collections.sort ( keys );

			for ( String key : keys )
			{
				final Message msg = entriesAtTime.get ( key );
				result.add ( msg );
			}
		}

		return result;
	}

	private void flush ()
	{
		if ( fRptTo == null ) return;

		final LinkedList<Long> timestamps = new LinkedList<> ( fSet.keySet () );
		Collections.sort ( timestamps );

		for ( Long ts : timestamps )
		{
			if ( fLastTs < 0 )
			{
				fLastTs = ts;
			}
			else
			{
				for ( long tsExpected : Period.getTimestampsBetween ( fSize, fLastTs, ts ) )
				{
					final Message msg = new Message ( new JSONObject().put ( "timestamp", tsExpected ).put ( "value", 0 ) );
					fRptTo.submit ( msg.toJson () );
				}
			}

			final HashMap<String,Message> entriesAtTime = fSet.get ( ts );
			fLastTs = ts;

			final LinkedList<String> keys = new LinkedList<> ( entriesAtTime.keySet () );
			Collections.sort ( keys );

			for ( String key : keys )
			{
				final Message msg = entriesAtTime.get ( key );
				fRptTo.submit ( msg.toJson () );
			}
		}

		fSet.clear ();
	}

	private final Period fSize;
	private final long fOffsetSeconds;
	private final HashMap<Long,HashMap<String,Message>> fSet;
	private final MessageBridge fBridge;
	private JsonObjectStreamSource fRptTo;
	private final String fRptToName;
	private long fLastTs = -1;

	public static final String kHost = "host";
	public static final String kMetricName = "metric";
	public static final String kTimestamp = "timestamp";
	public static final String kValue = "value";

	public static final String kCount = "count";

	public static class StdMsgBridge implements MessageBridge 
	{
		public StdMsgBridge ( StdDataCombiner sdc )
		{
			fCombiner = sdc;
		}

		@Override
		public long getTimestamp ( Message m ) { return m.getLong ( kTimestamp, -1 ); }

		@Override
		public String getKey ( Message m ) { return m.getValueAsString ( kMetricName ); }

		@Override
		public Message cloneWithTime ( long tsBucket, Message m )
		{
			return m.clone ().putValue ( kTimestamp, tsBucket );
		}

		@Override
		public Message merge ( Message origEntry, Message newEntry )
		{
			final long origCount = origEntry.getLong ( kCount, 1 );
			final double origVal = origEntry.getDouble ( kValue, 0 );
			final long newCount = newEntry.getLong ( kCount, 1 );
			final double newVal = newEntry.getDouble ( kValue, 0 );

			final Message base = origEntry.clone ();
			
			switch ( fCombiner )
			{
				case AVERAGE:
				{
					final double origTotal = origVal * origCount;
					final double addlTotal = newVal * newCount;
					final long totalCount = origCount + newCount;
					final double avgValue = totalCount == 0 ? 0 : ( origTotal + addlTotal ) / totalCount;
					return base
						.putValue ( kValue, avgValue )
						.putValue ( kCount, totalCount )
					;
				}

				case SUM:
				{
					return base
						.putValue ( kValue, origVal + newVal )
					;
				}

				default:
					return origEntry;
			}
		}

		private final StdDataCombiner fCombiner;
	}

	private static final Logger log = LoggerFactory.getLogger ( BucketingService.class );
}
