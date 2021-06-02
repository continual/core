package io.continual.onap.services.mrCommon;

import java.util.LinkedList;

import org.junit.Test;

import junit.framework.TestCase;

public class HostSelectorTest extends TestCase
{
	@Test
	public void testFailures ()
	{
		final HostSelector hs = HostSelector.builder ()
			.withHost ( "A" )
			.withHost ( "B" )
			.withHost ( "C" )
			.useInGivenOrder ()
			.build ()
		;

		final LinkedList<String> hosts = new LinkedList<> ();
		hs.copyInto ( hosts );

		assertEquals ( 3, hosts.size () );
		assertEquals ( "A", hosts.get ( 0 ) );
		assertEquals ( "B", hosts.get ( 1 ) );
		assertEquals ( "C", hosts.get ( 2 ) );

		hs.demote ( "B" );
		hs.copyInto ( hosts );

		assertEquals ( 3, hosts.size () );
		assertEquals ( "A", hosts.get ( 0 ) );
		assertEquals ( "C", hosts.get ( 1 ) );
		assertEquals ( "B", hosts.get ( 2 ) );
	}
}
