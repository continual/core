package io.continual.metrics;

import io.continual.services.Service;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

/**
 * The metrics service tracks and optionally publishes metrics information for the process.
 */
public interface MetricsService extends Service
{
	default MetricsCatalog getCatalog ( String string )
	{
		return getCatalog ( Path.getRootPath ().makeChildItem ( Name.fromString ( string ) ) );
	}
	
	MetricsCatalog getCatalog ( Path path );
}
