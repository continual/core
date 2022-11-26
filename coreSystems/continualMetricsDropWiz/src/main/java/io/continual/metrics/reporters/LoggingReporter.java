package io.continual.metrics.reporters;

import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.metrics.DropWizardMetricsService;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;

public class LoggingReporter extends SimpleService
{
	public LoggingReporter ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fMetrics = sc.get ( "metrics", DropWizardMetricsService.class );
		if ( fMetrics == null )
		{
			throw new BuildFailure ( "No metrics catalog in metrics LoggingReporter." );
		}
		
		fRunFlag = new AtomicBoolean ( true );
		fMetricsDumper = new MetricsDumpThread ( config.optLong ( "periodMs", 15*1000L ) );
	}

	@Override
	protected void onStartRequested () throws FailedToStart
	{
		fMetricsDumper.start ();
	}

	@Override
	protected void onStopRequested ()
	{
		fRunFlag.set ( false );
	}

	private class MetricsDumpThread extends Thread
	{
		public MetricsDumpThread ( long periodMs )
		{
			super ( "processor metrics dumper" );
			setDaemon ( true );
			fPeriodMs = periodMs;
		}
	
		@Override
		public void run ()
		{
			try
			{
				while ( fRunFlag.get () )
				{
					Thread.sleep ( fPeriodMs );
					log.info ( fMetrics.toJson().toString() );
				}
				log.info ( "Metrics dump thread exiting." );
			}
			catch ( InterruptedException e )
			{
				log.warn ( "Metrics dumper interrupted: ", e );
			}
			catch ( Throwable e )
			{
				log.warn ( "Metrics dumper terminated: ", e );
			}
		}

		private final long fPeriodMs;
	}

	private final DropWizardMetricsService fMetrics;
	private final AtomicBoolean fRunFlag;
	private final MetricsDumpThread fMetricsDumper;

	private static final Logger log = LoggerFactory.getLogger ( LoggingReporter.class );
}
