package io.continual.metrics.metricTypes;

public interface Timer
{
	interface Context extends AutoCloseable
	{
		/**
		 * Stop this timer and return the elapsed time in nanoseconds.
		 * @return
		 */
		long stop ();

		default void close () { stop (); }
	}

	Context time ();
}
