package io.continual.metrics;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.metrics.impl.StdMetricsCatalog;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.naming.Path;

/**
 * The metrics service tracks and optionally publishes metrics information for the process.
 */
public class DropWizardMetricsService extends SimpleService implements MetricsService
{
	public DropWizardMetricsService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fReg = new MetricRegistry (); 
		fReporters = new LinkedList<ScheduledReporter> ();
		
		// build any reporters requested by the configuration
		JsonVisitor.forEachElement ( config.optJSONArray ( "reporters" ), new ArrayVisitor<JSONObject,BuildFailure> ()
		{
			@Override
			public boolean visit ( JSONObject reporterData ) throws JSONException, BuildFailure
			{
				if ( reporterData.optBoolean ( "enabled", true ) )
				{
					Builder
						.withBaseClass ( ReporterBuilder.class )
						.withClassNameInData ()
						.usingData ( reporterData )
						.providingContext ( DropWizardMetricsService.this )
						.build ()
					;
					
					fReporters.add ( Builder.fromJson ( ScheduledReporter.class, reporterData ) );
				}
				return true;
			}
		} );
	}

	/**
	 * Get the metric registry
	 * @return a metric registry
	 */
	@Deprecated
	public MetricRegistry getRegistry ()
	{
		return fReg;
	}

	public MetricsCatalog getCatalog ( Path path )
	{
		return new StdMetricsCatalog ( fReg, path );
	}

	@Override
	protected void onStartRequested () throws FailedToStart
	{
		super.onStartRequested ();

		for ( ScheduledReporter r : fReporters )
		{
			r.start ( 1, TimeUnit.MINUTES );
		}
	}

	@Override
	protected void onStopRequested ()
	{
		for ( ScheduledReporter r : fReporters )
		{
			r.stop ();
		}

		super.onStopRequested ();
	}

	public JSONObject toJson ()
	{
		return toJson ( fReg );
	}

	public static JSONObject toJson ( MetricRegistry reg )
	{
		final JSONObject result = new JSONObject ();

		for ( Map.Entry<String,Metric> e : reg.getMetrics ().entrySet () )
		{
			final String key = e.getKey ();
			final String[] parts = key.split ( "\\." );
			JSONObject target = result;
			for ( int i=0; i<parts.length - 1; i++ )
			{
				Object tmp = target.opt ( parts[i] );
				if ( tmp == null )
				{
					tmp = new JSONObject ();
					target.put ( parts[i], tmp );
				}
				else if ( ! ( tmp instanceof JSONObject ) )
				{
					throw new IllegalStateException ( "Overlapping metric names at " + key );
					// FIXME: can we do something reasonable here?
				}
				target = (JSONObject) tmp;
			}
			target.put ( parts[parts.length-1], toJson(e.getValue () ) );
		}
		return result;
	}

	public static JSONObject toJson ( Metric m )
	{
		final JSONObject result = new JSONObject ();
		if ( m instanceof Counter )
		{
			final Counter mm = (Counter)m;
			result.put ( "count", mm.getCount () );
		}
		else if ( m instanceof Gauge )
		{
			final Gauge<?> mm = (Gauge<?>)m;
			result.put ( "value", mm.getValue ().toString () );
		}
		else if ( m instanceof Meter )
		{
			final Meter mm = (Meter)m;
			result
				.put ( "count", mm.getCount () )
				.put ( "rateMean", mm.getMeanRate () )
				.put ( "rate1Min", mm.getOneMinuteRate () )
				.put ( "rate5Min", mm.getFiveMinuteRate () )
				.put ( "rate15Min", mm.getFifteenMinuteRate () )
			;
		}
		else if ( m instanceof Timer )
		{
			final Timer mm = (Timer)m;
			result
				.put ( "count", mm.getCount () )
				.put ( "rateMean", mm.getMeanRate () )
				.put ( "rate1Min", mm.getOneMinuteRate () )
				.put ( "rate5Min", mm.getFiveMinuteRate () )
				.put ( "rate15Min", mm.getFifteenMinuteRate () )
				.put ( "snapshot", renderSnapshot ( mm.getSnapshot () ) )
			;
		}
		else if ( m instanceof Histogram )
		{
			final Histogram mm = (Histogram)m;
			result
				.put ( "count", mm.getCount () )
				.put ( "snapshot", renderSnapshot ( mm.getSnapshot () ) )
			;
		}
		else
		{
			result.put ( "value", "unknown metric type " + m.getClass ().getCanonicalName () );
		}
		return result;
	}

	private static JSONObject renderSnapshot ( Snapshot s )
	{
		return new JSONObject ()
			.put ( "min", s.getMin () )
			.put ( "max", s.getMax () )
			.put ( "mean", s.getMean () )
			.put ( "median", s.getMedian () )
			.put ( "stddev", s.getStdDev () )
			.put ( "pct75", s.get75thPercentile () )
			.put ( "pct95", s.get95thPercentile () )
			.put ( "pct98", s.get98thPercentile () )
			.put ( "pct99", s.get99thPercentile () )
			.put ( "pct999", s.get999thPercentile () )
		;
	}

	private final MetricRegistry fReg;
	private final LinkedList<ScheduledReporter> fReporters;
}
