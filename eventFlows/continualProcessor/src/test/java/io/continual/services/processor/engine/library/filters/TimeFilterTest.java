package io.continual.services.processor.engine.library.filters;

import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.impl.noop.NoopMetricsCatalog;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Sink;
import io.continual.services.processor.engine.model.Source;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.data.exprEval.ExprDataSource;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.JsonEval;
import io.continual.util.time.Clock;
import junit.framework.TestCase;

public class TimeFilterTest extends TestCase
{
	@Test
	public void testEmptyTimeFilter () throws BuildFailure
	{
		final TimeFilter f = new TimeFilter ( null, new JSONObject ()
			.put ( "expr", "${time}" )
		);

		final MessageProcessingContext msg = makeMessage ( new JSONObject ().put ( "time", 1L ));
		assertTrue ( f.passes ( msg  ) );
	}

	@Test
	public void testTimeAtLeastAbsFilter () throws BuildFailure
	{
		final TimeFilter f = new TimeFilter ( null, new JSONObject ()
			.put ( "expr", "${time}" )
			.put ( "lowerLimit", "12345" )
		);

		final MessageProcessingContext msg = makeMessage ( new JSONObject ().put ( "time", 1L ));
		assertFalse ( f.passes ( msg ) );

		final MessageProcessingContext msg2 = makeMessage ( new JSONObject ().put ( "time", 12346L ));
		assertTrue ( f.passes ( msg2 ) );
	}

	@Test
	public void testTimeAtLeastRelFilter () throws BuildFailure
	{
		final long baseTimeMs = 14000000000L;
		Clock.useNewTestClock ()
			.set ( baseTimeMs )
		;

		final TimeFilter f = new TimeFilter ( null, new JSONObject ()
			.put ( "expr", "${time}" )
			.put ( "lowerLimit", "-1h" )
		);

		// 5 hours ago
		final MessageProcessingContext msg1 = makeMessage ( new JSONObject ().put ( "time", baseTimeMs - (1000L*60*60*5) ));
		assertFalse ( f.passes ( msg1 ) );

		// 5 minutes ago
		final MessageProcessingContext msg2 = makeMessage ( new JSONObject ().put ( "time", baseTimeMs - (1000L*60*5) ));
		assertTrue ( f.passes ( msg2 ) );
	}

	private static MessageProcessingContext makeMessage ( JSONObject msgData )
	{
		final Message msg = Message.copyJsonToMessage ( msgData );
		final MetricsCatalog metrics = new NoopMetricsCatalog ();
		
		return new MessageProcessingContext ()
		{
			@Override
			public StreamProcessingContext getStreamProcessingContext () { return null; }

			@Override
			public String getId () { return "id"; }

			@Override
			public Message getMessage () { return msg; }

			@Override
			public boolean shouldContinue () { return true; }

			@Override
			public void stopProcessing () {}

			@Override
			public void warn ( String warningText ) {} 

			@Override
			public Source getSource ( String srcName ) { return null; }

			@Override
			public Sink getSink ( String sinkName ) { return null; }

			@SuppressWarnings("unchecked")
			@Override
			public <T> T evalExpression ( String expression, Class<T> targetType, ExprDataSource... addlSrcs  )
			{
				final ExprDataSource eds = new ExprDataSource ()
				{
					@Override
					public Object eval ( String label )
					{
						return JsonEval.eval ( msg.accessRawJson (), label );
					}
				};
				final String asString = ExpressionEvaluator.evaluateText ( expression, eds );
				if ( targetType.equals ( String.class ) )
				{
					return (T) asString;
				}
				if ( targetType.equals ( Long.class ) )
				{
					return (T) new Long ( Long.parseLong ( asString ) );
				}
				if ( targetType.equals ( Integer.class ) )
				{
					return (T) new Integer ( Integer.parseInt ( asString ) );
				}
				if ( targetType.equals ( Double.class ) )
				{
					return (T) new Double ( Double.parseDouble ( asString ) );
				}

				throw new IllegalArgumentException ( "Can't eval to " + targetType.getName () );
			}

			@Override
			public MetricsCatalog getMetrics () { return metrics; }
		};
	}
}
