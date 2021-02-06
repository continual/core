package io.continual.metrics.impl.noop;

import io.continual.metrics.metricTypes.Counter;

/**
 * No-op counter is a placeholder used when metrics generators do not yet have a target.
 */
public class NoopCounter implements Counter
{
	@Override
	public void increment ( long amount )
	{
	}

	@Override
	public long getCount ()
	{
		return 0;
	}
}
