package io.continual.services.processor.engine.library;

import org.json.JSONObject;

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

public class TestProcessingContext implements MessageProcessingContext
{
	public TestProcessingContext ( JSONObject msg )
	{
		this ( Message.copyJsonToMessage ( msg ) );
	}

	public TestProcessingContext ( Message msg )
	{
		fMsg = msg;
	}
	
	@Override
	public StreamProcessingContext getStreamProcessingContext () { return null; }

	@Override
	public String getId () { return "id"; }

	@Override
	public Message getMessage () { return fMsg; }

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
				return JsonEval.eval ( fMsg.accessRawJson (), label );
			}
		};
		final String asString = ExpressionEvaluator.evaluateText ( expression, eds );
		if ( targetType.equals ( String.class ) )
		{
			return (T) asString;
		}
		if ( targetType.equals ( Long.class ) )
		{
			return (T) Long.valueOf ( Long.parseLong ( asString ) );
		}
		if ( targetType.equals ( Integer.class ) )
		{
			return (T) Integer.valueOf ( Integer.parseInt ( asString ) );
		}
		if ( targetType.equals ( Double.class ) )
		{
			return (T) Double.valueOf ( Double.parseDouble ( asString ) );
		}

		throw new IllegalArgumentException ( "Can't eval to " + targetType.getName () );
	}

	@Override
	public MetricsCatalog getMetrics () { return fMetrics; }

	private final Message fMsg;
	private final MetricsCatalog fMetrics = new NoopMetricsCatalog ();
}
