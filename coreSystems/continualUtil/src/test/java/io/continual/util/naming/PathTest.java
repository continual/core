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
		SimpleTest.invalidPath ( "/foo/a.b/bar" ),
		SimpleTest.validPath ( "///huh", "/huh" ),
		SimpleTest.validPath ( "////", "/" ),
		SimpleTest.validPath ( "/./././.", "/" ),
		SimpleTest.validPath ( "/foo/.", "/foo" ),
		SimpleTest.invalidPath ( "foo/bar" ),
	};
}
