package io.continual.metrics.metricTypes;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface Timer
{
	void update ( long duration, TimeUnit unit );

	void update ( Duration duration );

	<T> T time ( Callable<T> event ) throws Exception;

	<T> T timeSupplier ( Supplier<T> event );

	void time ( Runnable event );

	interface Context
	{
		long stop ();
	}
	Context time ();
}
