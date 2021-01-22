package io.continual.metrics.impl.noop;

import io.continual.metrics.metricTypes.Histogram;

/**
 * No-op Histogram is a placeholder used when metrics generators do not yet have a target.
 */
public class NoopHistogram implements Histogram
{
	public void update ( int value ) {}

	public void update ( long value ) {}
}
