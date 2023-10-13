/*
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

package io.continual.http.service.framework.context;

import io.continual.http.service.framework.CHttpSession;
import io.continual.http.service.framework.inspection.CHttpObserver;
import io.continual.http.service.framework.inspection.impl.NoopInspector;
import io.continual.http.service.framework.routing.CHttpRequestRouter;

/**
 * The request context provides the request, the response, and the session, as well as 
 * access to a few other components.
 */
public abstract class CHttpRequestContext
{
	public void install ( CHttpObserver i )
	{
		fInspector = i == null ? new NoopInspector () : i ;
	}

	public void close ()
	{
		fInspector.closeTrx ();
	}

	/**
	 * Get the connection being serviced.
	 * @return a connection
	 */
	public CHttpSession session ()
	{
		return fSession;
	}

	/**
	 * Get the HTTP request
	 * @return an http request
	 */
	public abstract CHttpRequest request ();

	/**
	 * Get the HTTP response
	 * @return an http response
	 */
	public abstract CHttpResponse response ();

	/**
	 * Get the request router
	 * @return the request router
	 */
	protected CHttpRequestRouter router ()
	{
		return fRouter;
	}

	/**
	 * Get the attached inspector
	 * @return an inspector
	 */
	protected CHttpObserver inspector ()
	{
		return fInspector;
	}

	private final CHttpSession fSession;
	private final CHttpRequestRouter fRouter;
	private CHttpObserver fInspector;

	protected CHttpRequestContext ( CHttpSession s, CHttpRequestRouter router )
	{
		fSession = s;
		fRouter = router;
		fInspector = new NoopInspector ();
	}
}
