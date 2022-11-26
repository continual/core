package io.continual.metrics.impl;

import java.util.LinkedList;
import java.util.Set;

import org.json.JSONObject;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistry.MetricSupplier;

import io.continual.metrics.DropWizardMetricsService;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.metricTypes.Counter;
import io.continual.metrics.metricTypes.Gauge;
import io.continual.metrics.metricTypes.Histogram;
import io.continual.metrics.metricTypes.Meter;
import io.continual.metrics.metricTypes.Timer;
import io.continual.util.data.json.JsonSerialized;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class StdMetricsCatalog implements MetricsCatalog, JsonSerialized
{
	public static class Builder
	{
		public Builder atBasePath ( Path p )
		{
			basePath = p;
			return this;
		}

		public Builder usingRegistry ( MetricRegistry reg )
		{
			fReg = reg;
			return this;
		}
		
		public StdMetricsCatalog build ()
		{
			return new StdMetricsCatalog ( fReg, basePath );
		}

		private MetricRegistry fReg = new MetricRegistry ();
		private Path basePath = Path.getRootPath ();
	}

	public StdMetricsCatalog ( MetricRegistry actualRegistry )
	{
		this ( actualRegistry, Path.getRootPath () );
	}

	public StdMetricsCatalog ( MetricRegistry actualRegistry, Path basePath )
	{
		fReg = actualRegistry;
		fPathStack = new LinkedList<> ();
		fPathStack.add ( basePath );
	}

	/**
	 * Get a catalog "within" this one.
	 * @param name
	 * @return a catalog with scoped naming
	 */
	@Override
	public StdMetricsCatalog getSubCatalog ( Name name )
	{
		return new StdMetricsCatalog ( fReg, getCurrentBase().makeChildItem ( convertName ( name ) ) );
	}

	@Override
	public StdMetricsCatalog getSubCatalog ( String name )
	{
		return getSubCatalog ( Name.fromString ( name ) );
	}

	public StdMetricsCatalog removeSubCatalog ( Name name )
	{
		final String pathPrefix = convertPath ( getCurrentBase().makeChildItem ( convertName ( name ) ) ) + ".";

		// from here we want to remove all entries in the actual registry that start with the prefix
		final Set<String> metrics = fReg.getMetrics ().keySet ();
		for ( String metric : metrics )
		{
			if ( metric.startsWith ( pathPrefix ) )
			{
				fReg.remove ( metric );
			}
		}

		return this;
	}

	@Override
	public PathPopper push ( String name )
	{
		final Name n = Name.fromString ( name );
		final Path p = getCurrentBase().makeChildItem ( n );
		fPathStack.add ( p );

		return new PathPopper ()
		{
			@Override
			public void close ()
			{
				pop ();
			}
		};
	}

	@Override
	public void pop ()
	{
		if ( fPathStack.size () < 2 )
		{
			throw new IllegalStateException ( "You cannot pop from this metrics catalog." );
		}
		fPathStack.removeLast ();
	}
	
	@Override
	public void remove ( String name )
	{
		final Path fullPath = getCurrentBase().makeChildPath ( Path.getRootPath ().makeChildItem ( Name.fromString ( name ) ) );
		final String dotted = convertPath ( fullPath );
		fReg.remove ( dotted );
	}

	@Override
	public Counter counter ( Path name, String helpText )
	{
		final Path fullPath = getCurrentBase().makeChildPath ( name );
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
	public Meter meter ( Path name, String helpText )
	{
		final Path fullPath = getCurrentBase().makeChildPath ( name );
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
	public <T> Gauge<T> gauge ( Path name, String helpText, GaugeFactory<T> factory )
	{
		final Path fullPath = getCurrentBase().makeChildPath ( name );
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
	public Histogram histogram ( Path name, String helpText )
	{
		final Path fullPath = getCurrentBase().makeChildPath ( name );
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
	public Timer timer ( Path name, String helpText )
	{
		final Path fullPath = getCurrentBase().makeChildPath ( name );
		final com.codahale.metrics.Timer codaTimer = fReg.timer ( convertPath ( fullPath ) );

		return new Timer ()
		{
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
	public String toString ()
	{
		return toJson().toString ();
	}

	@Override
	public JSONObject toJson ()
	{
		JSONObject metrics = DropWizardMetricsService.toJson ( fReg );
		for ( Name name : getCurrentBase().getSegments () )
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
	private final LinkedList<Path> fPathStack;

	private String convertPath ( Path path )
	{
		// convert "/foo/bar/baz" to "foo.bar.baz"
		return path.toString ().replace ( '/', '.' ).substring ( 1 );
	}

	private Name convertName ( Name name )
	{
		// we don't want dots in name segments because dropwizard uses them to segment the path
		return Name.fromString ( name.toString ().replace ( '.', '_' ) );
	}

	public Path getBasePath ()
	{
		return getCurrentBase();
	}

	private Path getCurrentBase ()
	{
		return fPathStack.getLast ();
	}
}
