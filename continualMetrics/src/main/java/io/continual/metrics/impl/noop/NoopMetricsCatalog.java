package io.continual.metrics.impl.noop;

import org.json.JSONObject;

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
	public MetricsCatalog getSubCatalog ( Name name )
	{
		return this;
	}
	
	@Override
	public void remove ( String name )
	{
	}

	@Override
	public Counter counter ( Path name )
	{
		return new NoopCounter ();
	}

	@Override
	public Meter meter ( Path name )
	{
		return new NoopMeter ();
	}

	@Override
	public <T> Gauge<T> gauge ( Path name, GaugeFactory<T> factory )
	{
 		return new NoopGauge<T> ();
	}

	@Override
	public Histogram histogram ( Path name )
	{
		return new NoopHistogram ();
	}

	@Override
	public Timer timer ( Path name )
	{
		return new NoopTimer ();
	}

	@Override
	public JSONObject toJson ()
	{
		return new JSONObject ();
	}
}
