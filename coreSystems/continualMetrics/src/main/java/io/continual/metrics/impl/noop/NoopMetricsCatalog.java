package io.continual.metrics.impl.noop;

import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.metricTypes.Counter;
import io.continual.metrics.metricTypes.Gauge;
import io.continual.metrics.metricTypes.Histogram;
import io.continual.metrics.metricTypes.Meter;
import io.continual.metrics.metricTypes.Timer;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class NoopMetricsCatalog implements MetricsCatalog
{
	public NoopMetricsCatalog ( )
	{
	}

	@Override
	public NoopMetricsCatalog getSubCatalog ( Name name )
	{
		return this;
	}

	@Override
	public NoopMetricsCatalog removeSubCatalog ( Name name )
	{
		return this;
	}

	@Override
	public void remove ( String name )
	{
	}

	@Override
	public Counter counter ( Path name, String helpText )
	{
		return new NoopCounter ();
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
		return new NoopHistogram ();
	}

	@Override
	public Timer timer ( Path name, String helpText )
	{
		return new NoopTimer ();
	}

	@Override
	public PathPopper push ( String name )
	{
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
	}
}
