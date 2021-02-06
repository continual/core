
package io.continual.metrics.metricTypes;

public interface Histogram
{
	void update ( int value );

	void update ( long value );
}
