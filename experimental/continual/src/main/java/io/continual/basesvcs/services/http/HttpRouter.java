package io.continual.basesvcs.services.http;

import java.io.IOException;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.routing.DrumlinRequestRouter;
import io.continual.util.nv.NvReadable;

public interface HttpRouter
{
	/**
	 * Build the router for this service
	 * @param servlet
	 * @param rr
	 * @param p
	 * @throws IOException
	 * @throws BuildFailure 
	 */
	void setupRouter ( HttpServlet servlet, DrumlinRequestRouter rr, NvReadable p ) throws IOException, BuildFailure;
}
