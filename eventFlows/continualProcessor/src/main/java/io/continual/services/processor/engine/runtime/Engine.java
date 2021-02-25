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
import io.continual.services.processor.engine.library.util.SimpleMessageProcessingContext;
import io.continual.services.processor.engine.library.util.SimpleStreamProcessingContext;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.Pipeline;
import io.continual.services.processor.engine.model.Program;
import io.continual.services.processor.engine.model.Sink;
import io.continual.services.processor.engine.model.Source;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.services.processor.service.ProcessingService;
import io.continual.util.data.exprEval.ExprDataSource;
import io.continual.util.data.exprEval.ExprDataSourceStack;

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
			this ( name, s, SimpleStreamProcessingContext.builder ()
				.withSource ( s )
				.evaluatingAgainst ( fExprEvalStack )
				.loggingTo ( log )
				.build ()
			);
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

				final SimpleMessageProcessingContext.Builder mpcBuilder = SimpleMessageProcessingContext.builder ()
					.evaluatingAgainst ( fExprEvalStack )
					.serialNumbersFrom ( fSnGen )
					.sourcesAndSinksFrom ( fProgram )
					.usingContext ( fStreamContext )
				;
				
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
							pl.process ( mpcBuilder.build ( msgAndRoute.getMessage () ) );
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

	private static final Logger log = LoggerFactory.getLogger ( Engine.class );
}
