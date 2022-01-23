package io.continual.monitor.runner;

import io.continual.services.Service.FailedToStart;
import io.continual.services.processor.engine.library.filters.HasField;
import io.continual.services.processor.engine.library.processors.Log;
import io.continual.services.processor.engine.library.processors.SendToSink;
import io.continual.services.processor.engine.library.processors.ShiftDown;
import io.continual.services.processor.engine.library.sinks.RcvrSink;
import io.continual.services.processor.engine.library.sources.StdInSource;
import io.continual.services.processor.engine.model.Pipeline;
import io.continual.services.processor.engine.model.Program;
import io.continual.services.processor.engine.model.Rule;
import io.continual.services.processor.engine.runtime.Engine;
import io.continual.util.data.exprEval.EnvDataSource;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.standards.MimeTypes;

/**
 * This is a container-friendly program that uses environment settings for connection information
 * to a Rcvr instance and pushes lines from stdin to it.
 * 
 * (It does not require or depend on the continualMonitor interface, which is a Java specific
 * monitor program.)
 */
public class StdInToRcvr
{
	public static void main ( String[] args )
	{
		try
		{
			final String sinkName = "rcvr"; 

			final ExpressionEvaluator ee = new ExpressionEvaluator ( new EnvDataSource() );
			final RcvrSink sink = new RcvrSink.Builder ()
				.sendingTo ( ee.evaluateText ( "${CIO_RCVR_HOST}" ) )
				.onTopic ( ee.evaluateText ( "${CIO_RCVR_TOPIC}" ) )
				.onStream ( ee.evaluateText ( "${CIO_RCVR_STREAM}" ) )
				.asUser ( ee.evaluateText ( "${CIO_RCVR_USER}" ), ee.evaluateText ( "${CIO_RCVR_PASSWORD}" ) )
				.build ()
			;
			
			final Program program = new Program ()
				.addSource ( "stdin", new StdInSource () )
				.addSink ( sinkName, sink )
				.addPipeline ( Program.kDefaultPipeline, new Pipeline ()
					.addRule ( new Rule.Builder ()
						.checkIf ( new HasField ( MimeTypes.kAppJson ) )
						.thenDo ( new ShiftDown ( MimeTypes.kAppJson ) )
						.and ( new SendToSink ( sinkName ) )
						.build ()
					)
					.addRule ( new Rule.Builder ()
						.alwaysDo ( new Log () )
						.build ()
					)
				)
			;
			final Engine engine = new Engine ( program );
			engine.startAndWait ();
		}
		catch ( FailedToStart e )
		{
			System.err.println ( e.getMessage () );
		}
		catch ( Exception e )
		{
			System.err.println ( e.getMessage () );
			e.printStackTrace ( System.err );
		}
	}
}
