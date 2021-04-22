package io.continual.services.processor.engine.library.processors;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;
import io.continual.util.data.exprEval.ExprDataSource;
import io.continual.util.data.json.JsonVisitor;

public class SplitValue implements Processor
{
	public SplitValue ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		fPatternText = config.getString ( "pattern" );
		fPattern = Pattern.compile ( fPatternText );

		fValueExpr = config.getString ( "value" );

		fTargetFields = JsonVisitor.objectToMap ( config.getJSONObject ( "assignments" ) );
	}

	@Override
	public void process ( MessageProcessingContext ctx )
	{
		final String value = ctx.evalExpression ( fValueExpr );
		final Matcher m = fPattern.matcher ( value );
		if ( m.matches () )
		{
			ExprDataSource matcherSrc = new ExprDataSource ()
			{
				@Override
				public Object eval ( String label )
				{
					try
					{
						final int arg = Integer.parseInt ( label );
						if ( arg >= 0 && arg < m.groupCount () + 1 )
						{
							return m.group ( arg );
						}
					}
					catch ( NumberFormatException x )
					{
						// ignore: it could be a field reference, for example
					}
					return null;
				}
			};
			
			for ( Map.Entry<String,String> e : fTargetFields.entrySet () )
			{
				final String val = ctx.evalExpression ( e.getValue (), String.class, matcherSrc );
				ctx.getMessage ().putValue ( e.getKey (), val );
			}
		}
		// else: do nothing
	}

	private final String fPatternText;
	private final Pattern fPattern;
	private final String fValueExpr;
	private final Map<String,String> fTargetFields;
}
