package io.continual.metrics.impl.noop;

import io.continual.metrics.metricTypes.Meter;

/**
 * No-op meter is a placeholder used when metrics generators do not yet have a target.
 */
public class NoopMeter implements Meter
{
	@Override
	public void mark ( long amt )
	{
	}

	@Override
	public long getCount ()
	{
		return 0;
	}

	@Override
	public double getMeanRate ()
	{
		return 0;
	}

	@Override
	public double getOneMinuteRate ()
	{
		return 0;
	}

	@Override
	public double getFiveMinuteRate ()
	{
		return 0;
	}

	@Override
	public double getFifteenMinuteRate ()
	{
		return 0;
	}
}
