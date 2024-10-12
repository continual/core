package io.continual.services.processor.engine.library.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import io.continual.builder.Builder.BuildFailure;
import io.continual.metrics.MetricsCatalog;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Sink;
import io.continual.services.processor.engine.model.Source;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.services.processor.engine.runtime.SerialNumberGenerator;
import io.continual.util.data.exprEval.ExprDataSource;
import io.continual.util.data.exprEval.ExprDataSourceStack;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.JsonEval;

public class SimpleMessageProcessingContext implements MessageProcessingContext 
{
	public static class Builder  
	{
		/**
		 * Build a processing context for a specific message
		 * @param msg
		 * @return a message processing context
		 * @throws BuildFailure 
		 */
		public SimpleMessageProcessingContext build ( Message msg ) throws BuildFailure
		{
			return new SimpleMessageProcessingContext ( this, msg );
		}

		public Builder usingContext ( StreamProcessingContext s ) { fStreamProcContext = s; return this; }
		public Builder serialNumbersFrom ( SerialNumberGenerator sng ) { fSng = sng; return this; }
		public Builder evaluatingAgainst ( ExprDataSource eval ) { fEvalStack = eval; return this; }

		private StreamProcessingContext fStreamProcContext = null;
		private SerialNumberGenerator fSng = new SerialNumberGenerator ();
		private ExprDataSource fEvalStack = new ExprDataSourceStack ();
	}
	
	public static Builder builder ()
	{
		return new Builder ();
	}

	@Override
	public StreamProcessingContext getStreamProcessingContext ()
	{
		return fSpc;
	}

	@Override
	public Message getMessage ()
	{
		return fMsg;
	}

	@Override
	public String getId ()
	{
		return fId;
	}

	@Override
	public Source getSource ( String sinkName )
	{
		return getStreamProcessingContext().getProgram().getSources().get ( sinkName );
	}

	@Override
	public Sink getSink ( String sinkName )
	{
		return getStreamProcessingContext().getProgram().getSinks ().get ( sinkName );
	}

	public boolean shouldContinue ()
	{
		return !fHaltRequested && !fSpc.failed ();
	}

	@Override
	public void stopProcessing ()
	{
		fHaltRequested = true;
	}

	@Override
	public void stopProcessing ( String warningText )
	{
		fHaltRequested = true;
		warn ( warningText );
	}

	@Override
	public void warn ( String warningText )
	{
		fSpc.warn ( "msg #" + fId + ": " + warningText );
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T evalExpression ( String expression, Class<T> targetClass, ExprDataSource... addl )
	{
		final ExprDataSource eds = new ExprDataSource ()
		{
			@Override
			public Object eval ( String label )
			{
				return JsonEval.eval ( fMsg.accessRawJson (), label );
			}
		};

		final ExprDataSourceStack addlStack = new ExprDataSourceStack ( addl ); 

		final String asString = ExpressionEvaluator.evaluateText ( expression, addlStack, eds, fEvalStack );
		if ( targetClass.equals ( String.class ) )
		{
			return (T) asString;
		}
		if ( targetClass.equals ( Long.class ) )
		{
			return (T) Long.valueOf ( Long.parseLong ( asString ) );
		}
		if ( targetClass.equals ( Integer.class ) )
		{
			return (T) Integer.valueOf ( Integer.parseInt ( asString ) );
		}
		if ( targetClass.equals ( Double.class ) )
		{
			return (T) Double.valueOf ( Double.parseDouble ( asString ) );
		}
		if ( targetClass.equals ( JSONArray.class ) )
		{
			return (T) new JSONArray ( new JSONTokener ( asString ) );
		}
		if ( targetClass.equals ( JSONObject.class ) )
		{
			return (T) new JSONObject ( new JSONTokener ( asString ) );
		}

		throw new IllegalArgumentException ( "Can't eval to " + targetClass.getName () );
	}

	@Override
	public MetricsCatalog getMetrics ()
	{
		return fSpc.getMetrics ().getSubCatalog ( "messageProcessing" );
	}

	private SimpleMessageProcessingContext ( Builder b, Message msg ) throws BuildFailure
	{
		fSpc = b.fStreamProcContext;
		fMsg = msg;
		fId = b.fSng.getNext ();
		fEvalStack = b.fEvalStack;

		if ( fSpc == null ) throw new BuildFailure ( "No stream processing context in message processing context." );
	}

	private final StreamProcessingContext fSpc;
	private final String fId;
	private final Message fMsg;
	private final ExprDataSource fEvalStack;
	private boolean fHaltRequested = false;
}
