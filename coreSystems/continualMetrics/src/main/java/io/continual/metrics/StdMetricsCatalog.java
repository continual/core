package io.continual.metrics;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.json.JSONObject;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistry.MetricSupplier;

import io.continual.metrics.metricTypes.Counter;
import io.continual.metrics.metricTypes.Gauge;
import io.continual.metrics.metricTypes.Histogram;
import io.continual.metrics.metricTypes.Meter;
import io.continual.metrics.metricTypes.Timer;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

class StdMetricsCatalog implements MetricsCatalog
{
	public StdMetricsCatalog ( MetricRegistry actualRegistry )
	{
		this ( actualRegistry, Path.getRootPath () );
	}

	public StdMetricsCatalog ( MetricRegistry actualRegistry, Path basePath )
	{
		fReg = actualRegistry;
		fBasePath = basePath;
	}

	/**
	 * Get a catalog "within" this one.
	 * @param name
	 * @return a catalog with scoped naming
	 */
	@Override
	public MetricsCatalog getSubCatalog ( Name name )
	{
		return new StdMetricsCatalog ( fReg, fBasePath.makeChildItem ( name ) );
	}
	
	@Override
	public void remove ( String name )
	{
		final Path fullPath = fBasePath.makeChildPath ( Path.getRootPath ().makeChildItem ( Name.fromString ( name ) ) );
		final String dotted = convertPath ( fullPath );
		fReg.remove ( dotted );
	}

	@Override
	public Counter counter ( Path name )
	{
		final Path fullPath = fBasePath.makeChildPath ( name );
		final com.codahale.metrics.Counter codaCounter = fReg.counter ( convertPath ( fullPath ) );

		return new Counter ()
		{
			@Override
			public void increment ( long amount ) { codaCounter.inc ( amount ); }

			@Override
			public long getCount () { return codaCounter.getCount (); }
		};
	}

	@Override
	public Meter meter ( Path name )
	{
		final Path fullPath = fBasePath.makeChildPath ( name );
		final com.codahale.metrics.Meter codaMeter = fReg.meter ( convertPath ( fullPath ) );

		return new Meter ()
		{
			@Override
			public void mark ( long amt ) { codaMeter.mark ( amt ); }

			@Override
			public long getCount () { return codaMeter.getCount (); }

			@Override
			public double getMeanRate () { return codaMeter.getMeanRate (); }

			@Override
			public double getOneMinuteRate () { return codaMeter.getOneMinuteRate (); }

			@Override
			public double getFiveMinuteRate () { return codaMeter.getFiveMinuteRate (); }

			@Override
			public double getFifteenMinuteRate () { return codaMeter.getFifteenMinuteRate (); }
		};
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> Gauge<T> gauge ( Path name, GaugeFactory<T> factory )
	{
		final Path fullPath = fBasePath.makeChildPath ( name );
		final com.codahale.metrics.Gauge<T> codeGauge = fReg.gauge ( convertPath ( fullPath ), new MetricSupplier<com.codahale.metrics.Gauge> ()
		{
			@Override
			public com.codahale.metrics.Gauge<T> newMetric ()
			{
				Gauge<T> g = factory.makeNewGauage ();
				return new com.codahale.metrics.Gauge ()
				{
				    public T getValue()
				    {
				    	return g.getValue ();
				    }
				};
			}
		} );

 		return new Gauge<T> ()
		{
			@Override
			public T getValue ()
			{
				return codeGauge.getValue ();
			}
		};
	}

	@Override
	public Histogram histogram ( Path name )
	{
		final Path fullPath = fBasePath.makeChildPath ( name );
		final com.codahale.metrics.Histogram codaHistogram = fReg.histogram ( convertPath ( fullPath ) );
		
		return new Histogram ()
		{
			@Override
			public void update ( int value ) { codaHistogram.update ( value ); }

			@Override
			public void update ( long value ) { codaHistogram.update ( value ); }
		};
	}

	@Override
	public Timer timer ( Path name )
	{
		final Path fullPath = fBasePath.makeChildPath ( name );
		final com.codahale.metrics.Timer codaTimer = fReg.timer ( convertPath ( fullPath ) );

		return new Timer ()
		{
			@Override
			public void update ( long duration, TimeUnit unit ) { codaTimer.update ( duration, unit ); }

			@Override
			public void update ( Duration duration ) { codaTimer.update ( duration ); }

			@Override
			public <T> T time ( Callable<T> event ) throws Exception { return codaTimer.time ( event ); }

			@Override
			public <T> T timeSupplier ( Supplier<T> event ) { return codaTimer.timeSupplier ( event ); }

			@Override
			public void time ( Runnable event ) { codaTimer.time ( event ); }

			@Override
			public Context time ()
			{
				final com.codahale.metrics.Timer.Context ctx = codaTimer.time ();
				return new Context ()
				{
					@Override
					public long stop ()
					{
						return ctx.stop ();
					}
				};
			}
		};
	}

	@Override
	public JSONObject toJson ()
	{
		JSONObject metrics = MetricsService.toJson ( fReg );
		for ( Name name : fBasePath.getSegments () )
		{
			metrics = metrics.optJSONObject ( name.toString () );
			if ( metrics == null )
			{
				return new JSONObject ();
			}
		}
		return metrics;
	}

	private final MetricRegistry fReg;
	private final Path fBasePath;

	private String convertPath ( Path name )
	{
		return name.toString ().replace ( '/', '.' ).substring ( 1 );
	}
}
