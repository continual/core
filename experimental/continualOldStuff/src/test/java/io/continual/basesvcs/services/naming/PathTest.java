package io.continual.basesvcs.services.naming;

import io.continual.util.naming.Path;
import junit.framework.TestCase;

public class PathTest extends TestCase
{
	public void testChildPathBuild ()
	{
		final Path parent = Path.fromString ( "/foo/bar" );
		final Path child = Path.fromString ( "/bee/baz" );
		final Path full = parent.makeChildPath ( child );
		assertEquals ( "/foo/bar/bee/baz", full.toString () );
	}

	public void testChildPathBuild2 ()
	{
		final Path parent = Path.fromString ( "/foo/bar/" );
		final Path child = Path.fromString ( "/bee/baz" );
		final Path full = parent.makeChildPath ( child );
		assertEquals ( "/foo/bar/bee/baz", full.toString () );
	}
}
