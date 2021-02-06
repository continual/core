package io.continual.metrics;

public interface MetricsSupplier
{
	/**
	 * Populate the metrics set with component-specific metrics values. The registry is
	 * expected to be configured to use a namespace specific to this supplier, so the supplier
	 * can add, for example, a counter "myCounter" and expect it to be placed properly in the
	 * overall system metrics registry.
	 * 
	 * @param metrics
	 */
	void populateMetrics ( MetricsCatalog metrics );
}
