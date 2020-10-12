package io.continual.http.service.framework;

import io.continual.http.service.framework.context.CHttpRequest;
import io.continual.util.naming.Path;

public interface CHttpMetricNamer
{
	Path getMetricNameFor ( CHttpRequest req );
}
