package io.continual.http.service.framework.context;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class ServletRequestTools
{
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
}
