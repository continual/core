package io.continual.util.naming;

import org.junit.Test;

import junit.framework.TestCase;

public class PathTest extends TestCase
{
	@Test
	public void testPathStringParsing ()
	{
		for ( SimpleTest st : kParseTests )
		{
			try
			{
				final Path p = Path.fromString ( st.fInput );
				if ( !st.fIsValid )
				{
					fail ( st.fInput + " should not be valid" );
				}
				assertEquals ( st.fRendering, p.toString () );
			}
			catch ( IllegalArgumentException x )
			{
				if ( st.fIsValid )
				{
					fail ( st.fInput + " should be valid but threw: " + x.getMessage () );
				}
			}
		}
	}

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

	@Test
	public void testPathWithinParent ()
	{
		for ( Path[] pathSet : kParentPathTests )
		{
			final Path child = pathSet[0];
			final Path parent = pathSet[1];
			final Path expect = pathSet[2];

			final Path result = child.makePathWithinParent ( parent );
			assertEquals ( expect, result );
		}
	}

	@Test
	public void testMakeChildPath()
	{
		final Path current = Path.fromString( "/foo" );
		final Path child = Path.fromString( "/bar" );
		final Path expect = Path.fromString( "/foo/bar" ); 

		final Path result = current.makeChildPath(child);
		assertEquals( expect, result );
	}

	@Test
	public void testDepth()
	{
		final Path path = Path.fromString( "/foo/bar" );
		final int expect = 2;

		final int result = path.depth();
		assertEquals( expect, result );
	}

	@Test
	public void testMakeChildItem()
	{
		final Path path = Path.fromString ( "/foo/bar" );
		final Name name = new Name( "bee" );
		final Path expect = Path.fromString( "/foo/bar/bee" );

		final Path result = path.makeChildItem(name);
		assertEquals( expect, result );
	}

	@Test
	public void testObjectUtils()
	{
		final Path path = Path.fromString ( "/foo/bar" );

		assertTrue( path.equals( path ) );
		assertFalse( path.equals( null ) );
		assertFalse( path.equals( "bee" ) );

		assertEquals( path.hashCode(), Path.fromString( "/foo/bar" ).hashCode() );

		assertNotNull( path.getItemName() );

		assertNotNull( path.getId() );

		assertTrue( path.startsWith( "/foo" ) );
	}

	private static class SimpleTest
	{
		public static SimpleTest validPath ( String input )
		{
			return new SimpleTest ( input, input, true );
		}

		public static SimpleTest validPath ( String input, String rendering )
		{
			return new SimpleTest ( input, rendering, true );
		}

		public static SimpleTest invalidPath ( String input )
		{
			return new SimpleTest ( input, null, false );
		}

		private SimpleTest ( String input, String rendering, boolean valid )
		{
			fInput = input;
			fIsValid = valid;
			fRendering = rendering;
		}

		public final String fInput;
		public final boolean fIsValid;
		public final String fRendering;
	};

	private static SimpleTest[] kParseTests =
	{
		SimpleTest.invalidPath ( null ),
		SimpleTest.invalidPath ( "" ),
		SimpleTest.invalidPath ( "foo" ),
		SimpleTest.validPath ( "/" ),
		SimpleTest.validPath ( "/foo" ),
		SimpleTest.validPath ( "/foo/bar" ),
		SimpleTest.validPath ( "/foo//bar", "/foo/bar" ),
		SimpleTest.validPath ( "/foo/./bar", "/foo/bar" ),
		SimpleTest.validPath ( "/foo/./bar/baz", "/foo/bar/baz" ),
		SimpleTest.invalidPath ( "/foo/../bar" ),
		SimpleTest.validPath ( "/foo/a.b/bar" ),
		SimpleTest.validPath ( "///huh", "/huh" ),
		SimpleTest.validPath ( "////", "/" ),
		SimpleTest.validPath ( "/./././.", "/" ),
		SimpleTest.validPath ( "/foo/.", "/foo" ),
		SimpleTest.invalidPath ( "foo/bar" ),
	};

	private static Path[][] kParentPathTests =
	{
		// { child, parent, expectedResult }
		{ Path.fromString ( "/foo/bar/bee" ), Path.fromString ( "/foo" ), Path.fromString ( "/bar/bee" ) },
		{ Path.fromString ( "/foo/bar/bee" ), Path.fromString ( "/" ), Path.fromString ( "/foo/bar/bee" ) },
		{ Path.fromString ( "/foo/bar/bee" ), Path.fromString ( "/foo/bar/bee" ), Path.fromString ( "/" ) },
		{ Path.fromString ( "/foo" ), Path.fromString ( "/" ), Path.fromString ( "/foo" ) },
	};
}
