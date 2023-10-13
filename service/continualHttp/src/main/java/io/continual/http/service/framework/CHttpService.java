/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.http.service.framework;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.inspection.CHttpObserverMgr;
import io.continual.iam.IamService;
import io.continual.metrics.MetricsCatalog;
import io.continual.metrics.MetricsService;
import io.continual.metrics.impl.noop.NoopMetricsCatalog;
import io.continual.services.Service;
import io.continual.services.ServiceContainer;

/**
 * The CHttpService is an abstract service that manages some common dependency services (e.g. accounts)
 * and also manages a list of route installers.
 */
public abstract class CHttpService implements Service
{
	public CHttpService ( ServiceContainer sc, JSONObject rawConfig ) throws BuildFailure
	{
		try
		{
			final JSONObject settings = sc.getExprEval ().evaluateJsonObject ( rawConfig );

			// the accounts service is optional...
			fAccounts = sc.getReqdIfNotNull ( settings.optString ( "accountService", null ), IamService.class );

			// the metrics service is optional...
			final MetricsService ms = sc.getReqdIfNotNull ( settings.optString ( "metricsService", null ), MetricsService.class );
			fMetrics = ms == null ? new NoopMetricsCatalog () : ms.getCatalog ( "http" );

			// the inspection service is also optional...
			fInspector = sc.getReqdIfNotNull ( settings.optString ( "inspector", null ), CHttpObserverMgr.class );
	
			fRouteInstallers = new LinkedList<> ();
			fFilters = new LinkedList<> ();
		}
		catch ( JSONException x )
		{
			throw new BuildFailure ( x );
		}
	}

	/**
	 * Get the accounts service, if registered.
	 * @return an accounts service or null
	 */
	@Deprecated
	protected IamService<?,?> getAccounts ()
	{
		return fAccounts;
	}

	/**
	 * Get metrics catalog
	 * @return a metrics catalog
	 */
	@Deprecated
	protected MetricsCatalog getMetrics ()
	{
		return fMetrics;
	}

	/**
	 * Get an inspector if registered
	 * @return an inspector or null
	 */
	@Deprecated
	protected CHttpObserverMgr getInspector ()
	{
		return fInspector;
	}

	/**
	 * Add a route installer.
	 * @param routeInstaller
	 * @return this
	 */
	public CHttpService addRouteInstaller ( CHttpRouteInstaller routeInstaller )
	{
		fRouteInstallers.add ( routeInstaller );
		return this;
	}

	/**
	 * Get the registered route installers.
	 * @return a list of 0 or more route installers.
	 */
	protected List<CHttpRouteInstaller> getRouteInstallers ()
	{
		return Collections.unmodifiableList ( fRouteInstallers );
	}

	/**
	 * Add a filter.
	 * @param filter
	 * @return this
	 */
	public CHttpService addRequestFilter ( CHttpFilter filter )
	{
		fFilters.add ( filter );
		return this;
	}

	/**
	 * Get the registered filters
	 * @return a list of 0 or more filters
	 */
	protected List<CHttpFilter> getFilters ()
	{
		return Collections.unmodifiableList ( fFilters );
	}

	private final IamService<?,?> fAccounts;
	private final MetricsCatalog fMetrics;
	private final CHttpObserverMgr fInspector;

	private final LinkedList<CHttpFilter> fFilters;
	private final LinkedList<CHttpRouteInstaller> fRouteInstallers;
}
