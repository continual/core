package io.continual.metrics.impl.noop;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.continual.metrics.metricTypes.Timer;

/**
 * No-op timer is a placeholder used when metrics generators do not yet have a target.
 */
public class NoopTimer implements Timer
{
	@Override
	public void update ( long duration, TimeUnit unit )
	{
	}

	@Override
	public void update ( Duration duration )
	{
	}

	@Override
	public <T> T time ( Callable<T> event )
	{
		return null;
	}

	@Override
	public <T> T timeSupplier ( Supplier<T> event )
	{
		return null;
	}

	@Override
	public void time ( Runnable event )
	{
	}

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
