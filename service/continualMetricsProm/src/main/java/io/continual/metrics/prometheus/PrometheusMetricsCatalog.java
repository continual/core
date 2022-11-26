package io.continual.metrics.prometheus;

import java.util.HashMap;
import java.util.LinkedList;

import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.impl.noop.NoopGauge;
import io.continual.metrics.impl.noop.NoopMeter;
import io.continual.metrics.metricTypes.Counter;
import io.continual.metrics.metricTypes.Gauge;
import io.continual.metrics.metricTypes.Histogram;
import io.continual.metrics.metricTypes.Meter;
import io.continual.metrics.metricTypes.Timer;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class PrometheusMetricsCatalog implements MetricsCatalog
{
	public PrometheusMetricsCatalog ()
	{
		this ( Path.getRootPath () );
	}

	@Override
	public MetricsCatalog getSubCatalog ( Name name )
	{
		PrometheusMetricsCatalog result = fChildren.get ( name );
		if ( result != null )
		{
			return result;
		}
		
		result = new PrometheusMetricsCatalog ( fBasePath.makeChildItem ( name ) );
		fChildren.put ( name, result );
		return result;
	}

	@Override
	public MetricsCatalog removeSubCatalog ( Name name )
	{
		fChildren.remove ( name );
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
		// TODO Auto-generated method stub
	}

	@Override
	public Counter counter ( Path name, String helpText )
	{
		final Path fullPath = getCurrentBase().makeChildPath ( name );

		final String pName = pathToPrometheusName ( fullPath );
		final io.prometheus.client.Counter pc = io.prometheus.client.Counter.build ()
			.name ( pName )
			.help ( helpText )
			.create ()
		;
		register ( pName, pc );
		
		return new Counter ()
		{
			@Override
			public void increment ( long amount )
			{
				pc.inc ( amount );
			}

			@Override
			public long getCount ()
			{
				return Math.round ( pc.get () );
			}
		};
	}

	@Override
	public Meter meter ( Path name, String helpText )
	{
		return new NoopMeter ();
	}

	@Override
	public <T> Gauge<T> gauge ( Path name, String helpText, GaugeFactory<T> factory )
	{
		return new NoopGauge<T> ();
	}

	@Override
	public Histogram histogram ( Path name, String helpText )
	{
		final Path fullPath = getCurrentBase().makeChildPath ( name );
		final String pName = pathToPrometheusName ( fullPath );

		io.prometheus.client.Histogram h;
		io.prometheus.client.SimpleCollector<?> sc = fLocalPaths.get ( pName );
		if ( sc != null )
		{
			h = (io.prometheus.client.Histogram)sc;
		}
		else
		{
			h = io.prometheus.client.Histogram.build ()
				.name ( pName )
				.help ( helpText )
				.create ()
			;
			register ( pName, h );
		}

		return new Histogram ()
		{
			@Override
			public void update ( int value )
			{
				h.observe ( value );
			}

			@Override
			public void update ( long value )
			{
				h.observe ( value );
			}
		};
	}

	@Override
	public Timer timer ( Path name, String helpText )
	{
		final Path fullPath = getCurrentBase().makeChildPath ( name );
		final String pName = pathToPrometheusName ( fullPath );

		io.prometheus.client.Histogram h;
		io.prometheus.client.SimpleCollector<?> sc = fLocalPaths.get ( pName );
		if ( sc != null )
		{
			h = (io.prometheus.client.Histogram)sc;
		}
		else
		{
			h = io.prometheus.client.Histogram.build ()
				.name ( pName )
				.help ( helpText )
				.create ()
			;
			register ( pName, h );
		}

		return new Timer ()
		{
			@Override
			public Context time ()
			{
				final long startNs = System.nanoTime ();
				return new Context ()
				{
					@Override
					public long stop ()
					{
						final long endNs = System.nanoTime ();
						final long durationNs = endNs - startNs;
						h.observe ( durationNs );
						return durationNs;
					}
				};
			}
		};
	}

	private final Path fBasePath;
	private final LinkedList<Path> fPathStack;
	private final HashMap<String,io.prometheus.client.SimpleCollector<?>> fLocalPaths;
	private final HashMap<Name,PrometheusMetricsCatalog> fChildren;

	private PrometheusMetricsCatalog ( Path basePath )
	{
		fBasePath = basePath;

		fPathStack = new LinkedList<> ();
		fPathStack.add ( basePath );

		fChildren = new HashMap<> ();
		fLocalPaths = new HashMap<> ();
	}

	private Path getCurrentBase ()
	{
		return fPathStack.getLast ();
	}

	private void register ( String pName, io.prometheus.client.SimpleCollector<?> sc )
	{
		if ( !fLocalPaths.containsKey ( pName ) )
		{
			sc.register ();
			fLocalPaths.put ( pName, sc );
		}
	}

	private String pathToPrometheusName ( Path fullPath )
	{
		final StringBuilder sb = new StringBuilder ();
		for ( Name name : fullPath.getSegments () )
		{
			if ( sb.length () > 0 )
			{
				sb.append ( '_' );
			}

			String nameComponent = name.toString ();
			nameComponent = nameComponent
				.replaceAll ( " ", "" )
				.replaceAll ( "\\{", "" )
				.replaceAll ( "\\}", "" )
			;
			
			sb.append ( nameComponent );
		}
		return sb.toString ();
	}
}
