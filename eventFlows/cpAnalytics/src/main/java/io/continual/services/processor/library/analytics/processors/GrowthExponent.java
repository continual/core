package io.continual.services.processor.library.analytics.processors;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;

public class GrowthExponent implements Processor
{
	public GrowthExponent ( JSONObject config ) throws BuildFailure
	{
		this ( (ConfigLoadContext)null, config );
	}

	public GrowthExponent ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		fId = config.getString ( "entryId" );
		fTs = config.getString ( "timestamp" );
		fVal = config.getString ( "value" );
		fTimeUnit = TimeUnit.valueOf ( config.optString ( "rateTimeUnit", TimeUnit.DAYS.toString () ) );
		fToField = config.getString ( "resultTo" );
		fRange = config.optInt ( "range", -1 );
		fSeries = new LinkedList<> ();
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		final String id = context.evalExpression ( fId );
		final long ts = context.evalExpression ( fTs, Long.class );
		final double val = context.evalExpression ( fVal, Double.class );

		if ( ( fSeriesId == null || !id.equals ( fSeriesId ) ) )
		{
			// don't generate output for this record...
			context.getMessage ().putValue ( "invalid", true );

			if ( val > 0.0 )
			{
				// new baseline
				fSeriesId = id;
				fSeries.clear ();
				fSeries.add ( new Entry ( ts, val ) );

				if ( fSeriesId.equals ( "us.ny.onondaga" ) )
				{
					context.warn ( "hi" );
				}
			}
			else
			{
				// can't use this record
				fSeriesId = null;
			}
		}
		else
		{
			fSeries.add ( new Entry ( ts, val ) );

			// determine starting entry...
			final Entry start;
			if ( fRange < 0 )
			{
				start = fSeries.get ( 0 );
			}
			else if ( fSeries.size () < fRange )
			{
				// not enough data yet
				context.getMessage ().putValue ( "invalid", true );
				return;
			}
			else
			{
				start = fSeries.get ( fSeries.size () - fRange );
			}

			final long tsDiffSecs = ts - start.fTs;
			final long t = fTimeUnit.convert ( tsDiffSecs, TimeUnit.SECONDS );
			if ( t == 0.0 )
			{
				context.getMessage ().putValue ( "invalid", true );
				return;
			}

			final double A = val;
			final double A0 = start.fVal;

			final double eToTk = ( A / A0 );
			final double tk = Math.log ( eToTk );
			final double k = tk / t;

			context.warn ( fSeriesId + ": A: " + A + "; A0: " + A0 + "; tk: " + tk + "; k: " + k + "; t: " + t + "; ttd: " + ( 0.6931/k ) + " (range: " + fRange + ")" );

			if ( Double.isFinite ( k ) )
			{
				context.getMessage ()
					.putValue ( fToField, k )
					.putValue ( "invalid", false )
				;
			}
			else
			{
				context.getMessage ()
					.putValue ( fToField, 0 )
					.putValue ( "invalid", true )
				;
			}
		}
	}

	private final String fId;
	private final String fTs;
	private final String fVal;
	private final String fToField;
	private final TimeUnit fTimeUnit;
	private final int fRange;

	private String fSeriesId = null;

	private final LinkedList<Entry> fSeries;
	
	private class Entry
	{
		public Entry ( long ts, double val )
		{
			fTs = ts;
			fVal = val;
		}
		@Override
		public String toString () { return "" + fVal + "@" + fTs; }
		public final long fTs;
		public final double fVal;
	}
}
