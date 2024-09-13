package io.continual.flowcontrol.services.metrics;

import io.continual.metrics.MetricsCatalog;

/**
 * Metrics sender interface. Worker processes use this interface to deliver
 * metrics streams back to the control system.
 */
public interface MetricsSender
{
	void send ( MetricsCatalog mc );
}
