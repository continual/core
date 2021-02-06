
package io.continual.metrics.metricTypes;

public interface Meter
{
	default void mark ()
	{
		mark ( 1L );
	}

	void mark ( long amt );

	long getCount ();
	double getMeanRate ();
	double getOneMinuteRate ();
	double getFiveMinuteRate ();
	double getFifteenMinuteRate ();
}
