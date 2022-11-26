package io.continual.metrics.impl.noop;

import io.continual.metrics.metricTypes.Timer;

/**
 * No-op timer is a placeholder used when metrics generators do not yet have a target.
 */
public class NoopTimer implements Timer
{
	@Override
	public Context time ()
	{
		return new Context ()
		{
			@Override
			public long stop () { return 0; }
		};
	}
}
