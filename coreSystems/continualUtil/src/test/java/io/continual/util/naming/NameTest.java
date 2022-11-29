package io.continual.util.naming;

import org.junit.Test;

import junit.framework.TestCase;

public class NameTest extends TestCase
{
	@Test
	public void testMatches()
	{
		final Name name = new Name( "foobarbaz" );
		assertTrue( name.matches( "foo(.*)" ) );
	}

	@Test
	public void testCompareTo()
	{
		final Name name = new Name( "foo" );
		assertEquals( 0, name.compareTo( name ) );
	}
}
