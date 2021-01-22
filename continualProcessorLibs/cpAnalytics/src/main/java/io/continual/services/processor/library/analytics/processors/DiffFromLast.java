package io.continual.services.processor.library.analytics.processors;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;

public class DiffFromLast implements Processor
{
	public DiffFromLast ( JSONObject config ) throws BuildFailure
	{
		this ( (ConfigLoadContext)null, config );
	}

	public DiffFromLast ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		fId = config.getString ( "entryId" );
		fVal = config.getString ( "value" );
		fToField = config.getString ( "resultTo" );
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		final String id = context.evalExpression ( fId );
		final double val = context.evalExpression ( fVal, Double.class );

		if ( fLastId == null || !id.equals ( fLastId ) )
		{
			// new line...
			fLastId = id;
			context.getMessage ().putValue ( fToField, 0 );
		}
		else
		{
			context.getMessage ()
				.putValue ( fToField, val - fLastVal )
			;
		}

		fLastVal = val;
	}

	private final String fId;
	private final String fVal;
	private final String fToField;

	private String fLastId = null;
	private double fLastVal = 0;
}
