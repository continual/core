package io.continual.onap.services.mrCommon;

import java.io.IOException;

import org.junit.Test;

import io.continual.onap.services.subscriber.OnapMrFetchResponse;
import junit.framework.TestCase;

public class SimpleJsonResponseParserTest extends TestCase
{
	@Test
	public void testNullString () throws IOException
	{
		final OnapMrFetchResponse resp = new OnapMrFetchResponse ( 200, "OK" );
		new SimpleJsonResponseParser().parseResponseBody ( (String)null, resp );
		assertTrue ( resp.isEof () );
		assertEquals ( 0, resp.readyCount () );
	}

	@Test
	public void testEmptyString () throws IOException
	{
		final OnapMrFetchResponse resp = new OnapMrFetchResponse ( 200, "OK" );
		new SimpleJsonResponseParser().parseResponseBody ( "", resp );
		assertTrue ( resp.isEof () );
		assertEquals ( 0, resp.readyCount () );
	}

	@Test
	public void testEmptyArrayString () throws IOException
	{
		final OnapMrFetchResponse resp = new OnapMrFetchResponse ( 200, "OK" );
		new SimpleJsonResponseParser().parseResponseBody ( "[]", resp );
		assertTrue ( resp.isEof () );
		assertEquals ( 0, resp.readyCount () );
	}

	@Test
	public void testVariousBrokenStrings ()
	{
		for ( String in : kBrokenStrings )
		{
			try
			{
				final OnapMrFetchResponse resp = new OnapMrFetchResponse ( 200, "OK" );
				new SimpleJsonResponseParser().parseResponseBody ( in, resp );
				fail ( "Should have thrown." );
			}
			catch ( IOException e )
			{
				// expected
			}
		}
	}

	private static final String[] kBrokenStrings =
	{
		"[",
		"[}",
		"\\[",
		"[\"foo\",]",
		"[\"foo\\\"]",
	};
}
