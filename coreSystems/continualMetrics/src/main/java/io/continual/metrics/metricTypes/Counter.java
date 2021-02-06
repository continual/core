package io.continual.metrics.metricTypes;

public interface Counter
{
	default void increment () { increment(1); }
	default void decrement () { decrement(1); }
	default void decrement ( long amount ) { increment(-1L*amount); };

	void increment ( long amount );
	long getCount ();
}
