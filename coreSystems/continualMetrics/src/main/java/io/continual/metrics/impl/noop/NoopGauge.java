package io.continual.metrics.impl.noop;

import io.continual.metrics.metricTypes.Gauge;

/**
 * No-op meter is a placeholder used when metrics generators do not yet have a target.
 */
public class NoopGauge<T> implements Gauge<T>
{
	@Override
	public T getValue ()
	{
		return null;
	}
}
