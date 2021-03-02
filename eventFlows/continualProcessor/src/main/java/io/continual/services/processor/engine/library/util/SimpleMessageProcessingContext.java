package io.continual.services.processor.engine.library.util;

import io.continual.metrics.MetricsCatalog;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Program;
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
		 */
		public SimpleMessageProcessingContext build ( Message msg )
		{
			return new SimpleMessageProcessingContext ( this, msg );
		}

		public Builder usingContext ( StreamProcessingContext s ) { fStreamProcContext = s; return this; }
		public Builder serialNumbersFrom ( SerialNumberGenerator sng ) { fSng = sng; return this; }
		public Builder evaluatingAgainst ( ExprDataSource eval ) { fEvalStack = eval; return this; }
		public Builder sourcesAndSinksFrom ( Program prog ) { fSrcSinkProg = prog; return this; }

		private StreamProcessingContext fStreamProcContext = null;
		private SerialNumberGenerator fSng = new SerialNumberGenerator ();
		private ExprDataSource fEvalStack = new ExprDataSourceStack ();
		private Program fSrcSinkProg = new Program ();
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
		return fProgram.getSources().get ( sinkName );
	}

	@Override
	public Sink getSink ( String sinkName )
	{
		return fProgram.getSinks ().get ( sinkName );
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
	public <T> T evalExpression ( String expression, Class<T> targetClass )
	{
		final ExprDataSource eds = new ExprDataSource ()
		{
			@Override
			public Object eval ( String label )
			{
				return JsonEval.eval ( fMsg.accessRawJson (), label );
			}
		};
		final String asString = ExpressionEvaluator.evaluateText ( expression, eds, fEvalStack );
		if ( targetClass.equals ( String.class ) )
		{
			return (T) asString;
		}
		if ( targetClass.equals ( Long.class ) )
		{
			return (T) new Long ( Long.parseLong ( asString ) );
		}
		if ( targetClass.equals ( Integer.class ) )
		{
			return (T) new Integer ( Integer.parseInt ( asString ) );
		}
		if ( targetClass.equals ( Double.class ) )
		{
			return (T) new Double ( Double.parseDouble ( asString ) );
		}

		throw new IllegalArgumentException ( "Can't eval to " + targetClass.getName () );
	}

	@Override
	public MetricsCatalog getMetrics ()
	{
		return fSpc.getMetrics ().getSubCatalog ( "messageProcessing" );
	}

	private SimpleMessageProcessingContext ( Builder b, Message msg )
	{
		fSpc = b.fStreamProcContext;
		fMsg = msg;
		fId = b.fSng.getNext ();
		fEvalStack = b.fEvalStack;
		fProgram = b.fSrcSinkProg;
	}

	private final StreamProcessingContext fSpc;
	private final String fId;
	private final Message fMsg;
	private final ExprDataSource fEvalStack;
	private final Program fProgram;
	private boolean fHaltRequested = false;
}
