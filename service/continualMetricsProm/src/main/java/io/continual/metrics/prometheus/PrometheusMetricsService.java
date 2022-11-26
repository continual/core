package io.continual.metrics.prometheus;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.MetricsService;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class PrometheusMetricsService extends SimpleService implements MetricsService
{
	public PrometheusMetricsService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config );

		fTopLevel = new PrometheusMetricsCatalog ();
	}

	@Override
	public MetricsCatalog getCatalog ( Path path )
	{
		MetricsCatalog catalog = fTopLevel;
		for ( Name name : path.getSegmentList () )
		{
			catalog = catalog.getSubCatalog ( name );
		}
		return catalog;
	}

	private final PrometheusMetricsCatalog fTopLevel;
}
