package io.continual.metrics.reporters;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

import io.continual.metrics.MetricsService;
import io.continual.metrics.ReporterBuilder;

public class GraphiteReporterBuilder implements ReporterBuilder
{
	@SuppressWarnings("deprecation")
	public GraphiteReporterBuilder ( MetricsService svc, JSONObject graphiteConfig )
	{
		final Graphite graphite = new Graphite (
			new InetSocketAddress (
				graphiteConfig.optString ( "hostname", "localhost" ),
				graphiteConfig.optInt ( "port", 2003 )
			)
		);

		fReporter = GraphiteReporter
			.forRegistry ( svc.getRegistry () )
			.prefixedWith ( graphiteConfig.getString ( "metricsNodeName" ) )
			.convertRatesTo ( TimeUnit.SECONDS )
			.convertDurationsTo ( TimeUnit.MILLISECONDS )
			.filter ( MetricFilter.ALL )
			.build ( graphite )
		;
	}

	@Override
	public ScheduledReporter getReporter ()
	{
		return fReporter;
	}

	private final ScheduledReporter fReporter;
}
