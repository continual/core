package io.continual.services.processor.library.analytics.processors;

import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;

public class RateOfChange implements Processor
{
	public RateOfChange ( JSONObject config ) throws BuildFailure
	{
		this ( (ConfigLoadContext)null, config );
	}

	public RateOfChange ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		fId = config.getString ( "entryId" );
		fTs = config.getString ( "timestamp" );
		fVal = config.getString ( "value" );
		fTimeUnit = TimeUnit.valueOf ( config.optString ( "rateTimeUnit", TimeUnit.DAYS.toString () ) );
		fToField = config.getString ( "resultTo" );
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		final String id = context.evalExpression ( fId );
		final long ts = context.evalExpression ( fTs, Long.class );
		final double val = context.evalExpression ( fVal, Double.class );

		if ( fLastId == null || !id.equals ( fLastId ) )
		{
			// new line...
			fLastId = id;
			context.getMessage ().putValue ( "invalid", true );
		}
		else if ( fLastVal == 0.0 )
		{
			context.getMessage ().putValue ( "invalid", true );
		}
		else
		{
			final long tsDiffSecs = ts - fLastTs;
			final double valDiff = ( val - fLastVal ) / fLastVal;
			final long tsDiff = fTimeUnit.convert ( tsDiffSecs, TimeUnit.SECONDS );
			final double roc = ( valDiff / tsDiff );
			if ( Double.isFinite ( roc ) )
			{
				context.getMessage ()
					.putValue ( fToField, roc )
					.putValue ( "invalid", false )
				;
			}
			else
			{
				context.getMessage ()
					.putValue ( "invalid", false )
				;
			}
		}

		fLastTs = ts;
		fLastVal = val;
	}

	private final String fId;
	private final String fTs;
	private final String fVal;
	private final String fToField;
	private final TimeUnit fTimeUnit;

	private String fLastId = null;
	private long fLastTs = 0;
	private double fLastVal = 0;
}
