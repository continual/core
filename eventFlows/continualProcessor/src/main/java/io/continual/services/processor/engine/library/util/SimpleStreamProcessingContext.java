package io.continual.services.processor.engine.library.util;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.Source;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.util.data.exprEval.ExprDataSource;
import io.continual.util.data.exprEval.ExprDataSourceStack;
import io.continual.util.data.exprEval.ExpressionEvaluator;

public class SimpleStreamProcessingContext implements StreamProcessingContext 
{
	public static class Builder  
	{
		public SimpleStreamProcessingContext build ()
		{
			final SimpleStreamProcessingContext sspc = new SimpleStreamProcessingContext ( fSource, fEvalStack, fLog );
			for ( Map.Entry<String,Object> e : fData.entrySet () )
			{
				sspc.addNamedObject ( e.getKey (), e.getValue () );
			}
			return sspc;
		}

		public Builder withSource ( Source s ) { fSource = s; return this; }
		public Builder loggingTo ( Logger log ) { fLog = log; return this; }
		public Builder evaluatingAgainst ( ExprDataSource eval ) { fEvalStack = eval; return this; }
		public Builder holdingObject ( String key, Object obj ) { fData.put ( key, obj ); return this; }
		
		private Source fSource = null;
		private ExprDataSource fEvalStack = new ExprDataSourceStack ();
		private Logger fLog = defaultLog;
		private HashMap<String,Object> fData = new HashMap<> ();
	}
	
	public static Builder builder ()
	{
		return new Builder ();
	}
	
	@Override
	public void warn ( String warningText )
	{
		fLog.warn ( "stream: {}", warningText );
	}

	@Override
	public void fail ( String warningText )
	{
		warn ( warningText );
		fFailed = true;
	}

	@Override
	public boolean failed () { return fFailed; }
	
	@Override
	public StreamProcessingContext addNamedObject ( String name, Object o )
	{
		fObjects.put ( name, o );
		return this;
	}

	@Override
	public Object getNamedObject ( String name )
	{
		return fObjects.get ( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getNamedObject ( String name, Class<T> clazz ) throws ClassCastException
	{
		final Object o = getNamedObject ( name );
		if ( o == null ) return null;

		if ( !clazz.isInstance ( o ) )
		{
			throw new ClassCastException ( "Object " + name + " is not a " + clazz.getName () );
		}

		return (T) o;
	}

	@Override
	public <T> T getReqdNamedObject ( String name, Class<T> clazz ) throws NoSuitableObjectException
	{
		try
		{
			final T obj = getNamedObject ( name, clazz );
			if ( obj == null )
			{
				throw new NoSuitableObjectException ( "No object named " + name + "." );
			}
			return obj;
		}
		catch ( ClassCastException x )
		{
			throw new NoSuitableObjectException ( x );
		}
	}

	@Override
	public StreamProcessingContext removeNamedObject ( String name )
	{
		fObjects.remove ( name );
		return this;
	}

	@Override
	public boolean setFlag ( String flagName )
	{
		final boolean result = checkFlag ( flagName );
		addNamedObject ( flagName, Boolean.TRUE );
		return result;
	}

	@Override
	public boolean checkFlag ( String flagName )
	{
		final Boolean val = getNamedObject ( flagName, Boolean.class );
		if ( val == null ) return false;
		return val;
	}

	@Override
	public boolean clearFlag ( String flagName )
	{
		final boolean result = checkFlag ( flagName );
		removeNamedObject ( flagName );
		return result;
	}
	
	@Override
	public void requeue ( MessageAndRouting mr )
	{
		if ( fSource != null )
		{
			fSource.requeue ( mr );
		}
		else
		{
			warn ( "Cannot requeue a message without a source in context." );
		}
	}

	@Override
	public String evalExpression ( String expression )
	{
		return ExpressionEvaluator.evaluateText ( expression, fExprEvalStack );
	}

	private final Source fSource;
	private final HashMap<String,Object> fObjects;
	private boolean fFailed;
	private final ExprDataSource fExprEvalStack;
	private final Logger fLog;

	private static final Logger defaultLog = LoggerFactory.getLogger ( SimpleStreamProcessingContext.class );

	private SimpleStreamProcessingContext ( Source src, ExprDataSource exprEvalStack, Logger log )
	{
		fSource = src;
		fFailed = false;
		fObjects = new HashMap<> ();
		fExprEvalStack = exprEvalStack;
		fLog = log;
	}
}
