package io.continual.util.naming;

import org.junit.Test;

import junit.framework.TestCase;

public class PathTest extends TestCase
{
	@Test
	public void testPathComponents ()
	{
		final Path p = Path.fromString ( "/foo/bar/baz" );
		final Name[] names = p.getSegments ();
		assertEquals ( 3, names.length );
		assertEquals ( "foo", names[0].toString () );
		assertEquals ( "bar", names[1].toString () );
		assertEquals ( "baz", names[2].toString () );
	}
}
