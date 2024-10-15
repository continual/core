package io.continual.http.service.framework.context;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;

import org.junit.Test;

import junit.framework.TestCase;

public class ServletRequestToolsTest extends TestCase
{
	@Test
	public void testNonRouteable ()
	{
		assertTrue ( ServletRequestTools.isNonRouteable ( "10.0.0.1" ) );
		assertTrue ( ServletRequestTools.isNonRouteable ( "192.168.4.5" ) );
		assertFalse ( ServletRequestTools.isNonRouteable ( "13.0.1.2" ) );
	}

	@Test
	public void testNginxIngressSetup ()
	{
		final String sfRemoteAddr = "13.1.2.3";
		final String sfRemoteAddr2 = "14.1.2.3";

		// no forward header
		String remaddr = ServletRequestTools.getBestRemoteAddress ( new MockServletRequest ()
		{
			@Override
			public String getRemoteAddr ()
			{
				return sfRemoteAddr;
			}

			@Override
			public Enumeration<String> getHeaders ( String key )
			{
				return Collections.enumeration ( new LinkedList<> () );
			}
		} );
		assertEquals ( sfRemoteAddr, remaddr );

		// forward header with a routeable IP
		remaddr = ServletRequestTools.getBestRemoteAddress ( new MockServletRequest ()
		{
			@Override
			public String getRemoteAddr ()
			{
				return sfRemoteAddr;
			}

			@Override
			public Enumeration<String> getHeaders ( String key )
			{
				final LinkedList<String> result = new LinkedList<> ();
				if ( key != null && key.equals ( "X-Forwarded-For" ) )
				{
					result.add ( sfRemoteAddr2 );
				}
				return Collections.enumeration ( result );
			}
		} );
		assertEquals ( sfRemoteAddr2, remaddr );
	}
}
