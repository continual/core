package io.continual.metrics;

import com.codahale.metrics.ScheduledReporter;

public interface ReporterBuilder
{
	ScheduledReporter getReporter ();
}
