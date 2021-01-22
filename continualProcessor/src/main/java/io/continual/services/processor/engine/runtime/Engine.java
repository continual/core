/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.services.processor.engine.runtime;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.services.Service;
import io.continual.services.SimpleService;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Pipeline;
import io.continual.services.processor.engine.model.Program;
import io.continual.services.processor.engine.model.Sink;
import io.continual.services.processor.engine.model.Source;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.services.processor.service.ProcessingService;
import io.continual.util.data.exprEval.ExprDataSource;
import io.continual.util.data.exprEval.ExprDataSourceStack;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.json.JsonEval;

/**
 * An engine for message stream processing.
 */
public class Engine extends SimpleService implements Service
{
	/**
	 * Construct an engine to run a given program.
	 * @param p
	 */
	public Engine ( Program p )
	{
		fProgram = p;
		fThreads = new HashMap<> ();
		fSnGen = new SerialNumberGenerator ();
		fUserData = new HashMap<> ();

		for ( Map.Entry<String,Source> src : fProgram.getSources ().entrySet() )
		{
			fThreads.put ( src.getKey (), new ExecThread ( src.getKey (), src.getValue () ) );
		}

		fExprEvalStack = new ExprDataSourceStack (

			// user data...
			new ExprDataSource ()
			{
				@Override
				public Object eval ( String label )
				{
					return getUserData ( label );
				}
			},

			// environment...
			new ExprDataSource ()
			{
				@Override
				public Object eval ( String label )
				{
					return System.getenv ().get ( label );
				}
			}
		);
	}

	/**
	 * Is this engine running? (That is, does it have processing threads that have not yet exited?)
	 */
	@Override
	public synchronized boolean isRunning ()
	{
		for ( ExecThread t : fThreads.values () )
		{
			if ( t.isAlive () ) return true;
		}
		return false;
	}

	/**
	 * Start the engine and wait for its completion.
	 * @throws FailedToStart
	 */
	public void startAndWait () throws FailedToStart
	{
		try
		{
			start ();
			while ( isRunning () )
			{
				Thread.sleep ( 100 );
			}
		}
		catch ( InterruptedException e1 )
		{
			System.out.println ( "exiting..." );
		}

		waitForCompletion ();
	}

	public void waitForCompletion () 
	{
		// close sources and sinks
		for ( Source src : fProgram.getSources ().values () )
		{
			try
			{
				src.close ();
			}
			catch ( IOException e )
			{
				log.warn ( "Problem closing source: " + e.getMessage () );
			}
		}
		for ( Sink sink : fProgram.getSinks().values () )
		{
			try
			{
				sink.close ();
			}
			catch ( IOException e )
			{
				log.warn ( "Problem closing sink: " + e.getMessage () );
			}
		}
	}
	
	/**
	 * Get the stream context for a given source
	 * @param sourceName
	 * @return a stream context, or null if none exists
	 */
	public StreamProcessingContext getStreamContextFor ( String sourceName )
	{
		final ExecThread thread = fThreads.get ( sourceName );
		if ( thread != null )
		{
			return thread.getStreamContext ();
		}
		return null;
	}

	public Engine setUserData ( String key, String value )
	{
		fUserData.put ( key, value );
		return this;
	}

	public String getUserData ( String key )
	{
		return fUserData.get ( key );
	}
	
	public void removeUserData ( String key )
	{
		fUserData.remove ( key );
	}

	@Override
	protected void onStartRequested () throws FailedToStart
	{
		for ( Sink sink : fProgram.getSinks ().values () )
		{
			sink.init ( );
		}
		
		for ( ExecThread t : fThreads.values () )
		{
			t.start ();
		}
	}

	@Override
	protected void onStopRequested ()
	{
		log.info ( "Stopping processing engine..." );
		for ( ExecThread t : fThreads.values () )
		{
			try
			{
				t.getSource().close ();
			}
			catch ( IOException e )
			{
				log.warn ( "Problem closing source {}: {}", t.getSourceName (), e.getMessage () );
			}
		}
	}

	private final Program fProgram;
	private final HashMap<String,ExecThread> fThreads;
	private final SerialNumberGenerator fSnGen;
	private final HashMap<String,String> fUserData;
	private final ExprDataSourceStack fExprEvalStack;

	/**
	 * An execution thread to service message sources.
	 */
	private class ExecThread extends Thread
	{
		public ExecThread ( String name, Source s )
		{
			this ( name, s, new StdStreamProcessingContext ( s ) );
		}

		public ExecThread ( String name, Source s, StreamProcessingContext spc )
		{
			super ( "ExecThread " + name );

			fName = name;
			fSource = s;
			fStreamContext = spc;
		}

		public String getSourceName ()
		{
			return fName;
		}

		public Source getSource ()
		{
			return fSource;
		}

		public StreamProcessingContext getStreamContext ()
		{
			return fStreamContext;
		}

		@Override
		public void run ()
		{
			try
			{
				// add service objects and get them started
				for ( Map.Entry<String, ProcessingService> entry : fProgram.getServicesFor ( fName ).entrySet () )
				{
					fStreamContext.addNamedObject ( entry.getKey (), entry.getValue () );
				}
				for ( Map.Entry<String, ProcessingService> entry : fProgram.getServicesFor ( fName ).entrySet () )
				{
					entry.getValue ().startBackgroundProcessing ();
				}

				// while we have messages, push them through the pipeline
				log.info ( "Source " + fName + ": START" );
				while ( !fSource.isEof () && !fStreamContext.failed () )
				{
					final MessageAndRouting msgAndRoute = fSource.getNextMessage ( fStreamContext, 500, TimeUnit.MILLISECONDS );
					if ( msgAndRoute != null )
					{
						final Pipeline pl = fProgram.getPipeline ( msgAndRoute.getPipelineName () );
						if ( pl == null )
						{
							log.info ( "No pipeline {} for source \"{}\", ignored.", msgAndRoute.getPipelineName (), fName );
						}
						else
						{
							final StdProcessingContext pc = new StdProcessingContext ( fStreamContext, msgAndRoute.getMessage () );
							pl.process ( pc );
						}
						fSource.markComplete ( fStreamContext, msgAndRoute );
					}
				}
				if ( fSource.isEof () )
				{
					log.info ( "Source " + fName + ": EOF" );
				}
				else
				{
					log.warn ( "Processing stopped." );
				}
			}
			catch ( IOException e )
			{
				log.warn ( "Error on source {}: {}", fName, e.getMessage () );
			}
			catch ( InterruptedException e )
			{
				log.info ( "Source {} interrupted.", fName );
			}
			finally
			{
				for ( Map.Entry<String, ProcessingService> entry : fProgram.getServicesFor ( fName ).entrySet () )
				{
					entry.getValue ().stopBackgroundProcessing ();
				}
			}
		}
		
		private final String fName;
		private final Source fSource;
		private final StreamProcessingContext fStreamContext;
	}

	public class StdStreamProcessingContext implements StreamProcessingContext 
	{
		public StdStreamProcessingContext ( Source src )
		{
			fSource = src;
			fFailed = false;
			fObjects = new HashMap<> ();
		}

		@Override
		public void warn ( String warningText )
		{
			log.warn ( "stream: {}", warningText );
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
			fSource.requeue ( mr );
		}

		@Override
		public String evalExpression ( String expression )
		{
			return ExpressionEvaluator.evaluateText ( expression, fExprEvalStack );
		}

		private final Source fSource;
		private final HashMap<String,Object> fObjects;
		private boolean fFailed;
	}

	private class StdProcessingContext implements MessageProcessingContext 
	{
		public StdProcessingContext ( StreamProcessingContext spc, Message msg )
		{
			fSpc = spc;
			fMsg = msg;
			fId = fSnGen.getNext ();
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
			log.warn ( "msg #{}: {}", fId, warningText );
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
			final String asString = ExpressionEvaluator.evaluateText ( expression, eds, fExprEvalStack );
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

		private final StreamProcessingContext fSpc;
		private final String fId;
		private final Message fMsg;
		private boolean fHaltRequested = false;
	}

	private static final Logger log = LoggerFactory.getLogger ( Engine.class );
}
