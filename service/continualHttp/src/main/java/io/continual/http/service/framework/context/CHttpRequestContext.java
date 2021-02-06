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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import io.continual.http.service.framework.CHttpConnection;
import io.continual.http.service.framework.CHttpServlet;
import io.continual.http.service.framework.routing.CHttpRequestRouter;
import io.continual.util.nv.NvReadable;

/**
 * The request context provides the servlet, the inbound HTTP request, the outbound
 * HTTP response, the Connection, and other information to the request handlers provided by
 * the web application.
 */
public class CHttpRequestContext
{
	public CHttpRequestContext ( CHttpServlet webServlet, HttpServletRequest req, HttpServletResponse resp,
		CHttpConnection s, Map<String, Object> objects, CHttpRequestRouter router )
	{
		fRequest = req;
		fResponse = resp;
		fSession = s;
		fServlet = webServlet;
		fLocalContext = new HashMap<>();
		fObjects = objects;
		fRouter = router;

		fRequestImpl = new StdRequest ( fRequest );
	}

	/**
	 * Get the connection being serviced.
	 * @return a connection
	 */
	public CHttpConnection session ()
	{
		return fSession;
	}

	public NvReadable systemSettings ()
	{
		return fServlet.getSettings ();
	}

	public CHttpRequestRouter router ()
	{
		return fRouter;
	}

	public CHttpServlet getServlet ()
	{
		return fServlet;
	}

	public String servletPathToFullUrl ( String contentUrl )
	{
		final StringBuilder url = new StringBuilder ();

		final String scheme = fRequest.getScheme ().toLowerCase ();
		url.append ( scheme );
		url.append ( "://" );
		url.append ( fRequest.getServerName () );

		final int serverPort = fRequest.getServerPort ();
		if ( !( ( scheme.equals ( "http" ) && serverPort == 80 ) ||
			( scheme.equals ( "https" ) && serverPort == 443 ) ) )
		{
			url.append ( ":" );
			url.append ( serverPort );
		}

		final String path = servletPathToFullPath ( contentUrl );
		url.append ( path );

		log.info ( "calculated full URL for [" + contentUrl + "]: [" + url + "]" );
		return url.toString ();
	}

	public String servletPathToFullPath ( String contentUrl )
	{
		return servletPathToFullPath ( contentUrl, fRequest );
	}

	public static String servletPathToFullPath ( String contentUrl, HttpServletRequest req )
	{
		final StringBuilder sb = new StringBuilder ();

		final String contextPart = req.getContextPath ();
		sb.append ( contextPart );

		final String servletPart = req.getServletPath ();
		sb.append ( servletPart );

		sb.append ( contentUrl );

		log.info ( "calculated full path for [" + contentUrl + "]: context=[" + contextPart + "], servlet=["
			+ servletPart + "], result=[" + sb.toString () + "]" );
		return sb.toString ();
	}

	public Object object ( String key )
	{
		return fObjects.get ( key );
	}

	public CHttpRequest request ()
	{
		return fRequestImpl;
	}

	public CHttpResponse response ()
	{
		return new StdResponse ( fRequest, fResponse, fRouter );
	}

	private final HttpServletRequest fRequest;
	private final HttpServletResponse fResponse;
	final CHttpConnection fSession;
	private final CHttpServlet fServlet;
	final HashMap<String,Object> fLocalContext;
	private final Map<String, Object> fObjects;
	private final CHttpRequestRouter fRouter;

	private final CHttpRequest fRequestImpl;
	
	static org.slf4j.Logger log = LoggerFactory.getLogger ( CHttpRequestContext.class );
}
