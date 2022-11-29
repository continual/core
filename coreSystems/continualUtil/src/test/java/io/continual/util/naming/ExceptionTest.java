package io.continual.util.naming;

import org.junit.Test;

public class ExceptionTest
{
	@Test(expected = IllegalArgumentException.class)
	public void testInvalidMakePathWithinParent ()
	{
		final Path child = Path.fromString( "/foo/bar/bee" );
		final Path parent = Path.fromString( "/bar" );

		child.makePathWithinParent ( parent );
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidName ()
	{
		new Name( "/foo" );
	}
}
