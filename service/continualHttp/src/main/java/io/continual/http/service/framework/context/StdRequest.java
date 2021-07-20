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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import io.continual.http.service.framework.inspection.CHttpObserver;
import io.continual.http.service.framework.inspection.impl.NoopInspector;
import io.continual.util.data.TypeConvertor;

public class StdRequest implements CHttpRequest
{
	public StdRequest ( HttpServletRequest r )
	{
		this ( r, new NoopInspector () );
	}

	public StdRequest ( HttpServletRequest r, CHttpObserver inspector )
	{
		fRequest = r;
		fParamOverrides = new HashMap<>();
		fInspector = inspector;

		fInspector
			.method ( getMethod () )
			.onUrl ( getUrl () )
			.queryString ( getQueryString () )
			.withHeaders ( () -> getAllHeaders () )
		;
	}

	@Override
	public String getUrl ()
	{
		return fRequest.getRequestURL ().toString ();
	}

	@Override
	public boolean isSecure ()
	{
		return fRequest.isSecure ();
	}

	@Override
	public String getQueryString ()
	{
		final String qs = fRequest.getQueryString ();
		if ( qs != null && qs.length () == 0 ) return null;
		return qs;
	}
	
	@Override
	public String getMethod ()
	{
		return fRequest.getMethod ();
	}

	@Override
	public String getPathInContext ()
	{
		final String ctxPath = fRequest.getContextPath ();
		final int ctxPathLen = ctxPath.length();
		return fRequest.getRequestURI().substring ( ctxPathLen );
	}

	@Override
	public String getFirstHeader ( String h )
	{
		return getFirstHeader ( fRequest, h );
	}

	@Override
	public List<String> getHeader ( String h )
	{
		return getHeader ( fRequest, h );
	}

	@Override
	public Map<String,List<String>> getAllHeaders ()
	{
		return getAllHeaders ( fRequest );
	}
	
	@Override
	public String getContentType ()
	{
		final String result = fRequest.getContentType ();
		fInspector.contentTypeRequest ( result );
		return result;
	}

	@Override
	public int getContentLength ()
	{
		final int result = fRequest.getContentLength ();
		fInspector.contentLengthRequest ( result );
		return result;
	}

	@Override
	public InputStream getBodyStream () throws IOException
	{
		return fInspector.wrap ( fRequest.getInputStream () );
	}

	@Override
	public BufferedReader getBodyStreamAsText () throws IOException
	{
		return new BufferedReader ( new InputStreamReader ( getBodyStream () ) );
	}

	@Override
	public Map<String, String[]> getParameterMap ()
	{
		final HashMap<String,String[]> map = new HashMap<>();
		final Map<String,String[]> m = fRequest.getParameterMap ();

		map.putAll ( m );
		map.putAll ( fParamOverrides );

		return map;
	}

	@Override
	public String getParameter ( String key )
	{
		if ( fParamOverrides.containsKey ( key ) )
		{
			final String[] o = fParamOverrides.get ( key );
			return o.length > 0 ? o[0] : "";
		}
		else
		{
			return fRequest.getParameter ( key );
		}
	}

	@Override
	public String getParameter ( String key, String defVal )
	{
		String p = getParameter ( key );
		if ( p == null )
		{
			p = defVal;
		}
		return p;
	}

	@Override
	public int getIntParameter ( String key, int defVal )
	{
		int result = defVal;
		final String p = getParameter ( key );
		if ( p != null )
		{
			try
			{
				result = TypeConvertor.convertToInt ( p );
			}
			catch ( Exception x )
			{
				result = defVal;
			}
		}
		return result;
	}

	@Override
	public long getLongParameter ( String key, long defVal )
	{
		long result = defVal;
		final String p = getParameter ( key );
		if ( p != null )
		{
			try
			{
				result = TypeConvertor.convertToLong ( p );
			}
			catch ( Exception x )
			{
				result = defVal;
			}
		}
		return result;
	}

	@Override
	public double getDoubleParameter ( String key, double defVal )
	{
		double result = defVal;
		final String p = getParameter ( key );
		if ( p != null )
		{
			try
			{
				result = TypeConvertor.convertToDouble ( p );
			}
			catch ( Exception x )
			{
				result = defVal;
			}
		}
		return result;
	}

	@Override
	public boolean getBooleanParameter ( String key, boolean defVal )
	{
		boolean result = defVal;
		final String p = getParameter ( key );
		if ( p != null )
		{
			try
			{
				result = TypeConvertor.convertToBooleanBroad ( p );
			}
			catch ( Exception x )
			{
				result = defVal;
			}
		}
		return result;
	}

	@Override
	public void changeParameter ( String fieldName, String value )
	{
		fParamOverrides.put ( fieldName, new String[] { value } );
	}

	@Override
	public String getActualRemoteAddress ()
	{
		return getActualRemoteAddress ( fRequest );
	}

	@Override
	public String getBestRemoteAddress ()
	{
		return getBestRemoteAddress ( fRequest );
	}

	@Override
	public int getActualRemotePort ()
	{
		return fRequest.getRemotePort ();
	}

	@Override
	public int getBestRemotePort ()
	{
		return getBestRemotePort ( fRequest );
	}

	/**
	 * Get the actual remote addr (which could be a load balancer, for example)
	 * as address:port.
	 *  
	 * @param req
	 * @return
	 */
	public static String getActualRemoteAddress ( HttpServletRequest req )
	{
		return req.getRemoteAddr ();
	}

	public static int getActualRemotePort ( HttpServletRequest req )
	{
		return req.getRemotePort ();
	}

	public static String getBestRemoteAddress ( HttpServletRequest req )
	{
		final String fwdHost = getFirstHeader ( req, "X-Forwarded-For" );
		return fwdHost != null ? fwdHost : getActualRemoteAddress ( req );
	}

	public static int getBestRemotePort ( HttpServletRequest req )
	{
		final String fwdPort = getFirstHeader ( req, "X-Forwarded-Port" );
		if ( fwdPort != null )
		{
			// FIXME: do we care about throwing a parse exception?
			return Integer.parseInt ( fwdPort );
		}
		return getActualRemotePort ( req );
	}

	public static String getFirstHeader ( HttpServletRequest req, String h )
	{
		List<String> l = getHeader ( req, h );
		return ( l.size () > 0 ) ? l.iterator ().next () : null;
	}

	public static List<String> getHeader ( HttpServletRequest req, String h )
	{
		final LinkedList<String> list = new LinkedList<>();
		final Enumeration<?> e = req.getHeaders ( h );
		while ( e.hasMoreElements () )
		{
			list.add ( e.nextElement ().toString () );
		}
		return list;
	}

	public static Map<String,List<String>> getAllHeaders ( HttpServletRequest req )
	{
		final HashMap<String,List<String>> result = new HashMap<String,List<String>> ();

		final Enumeration<?> names = req.getHeaderNames ();
		if ( names != null )
		{
			while ( names.hasMoreElements () )
			{
				final String headerName = names.nextElement ().toString ();
				final LinkedList<String> list = new LinkedList<String> ();
				result.put ( headerName, list );

				final Enumeration<?> values = req.getHeaders ( headerName );
				if ( values != null )
				{
					while ( values.hasMoreElements () )
					{
						list.add ( values.nextElement ().toString () );
					}
				}
				// else: javadocs say some containers return null.
			}
		}
		// else: javadocs say some containers return null.

		return result;
	}

	private final HttpServletRequest fRequest;
	private final HashMap<String,String[]> fParamOverrides;
	private final CHttpObserver fInspector;
}
