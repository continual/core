package io.continual.resources.sources;

import java.io.IOException;
import java.io.File;

import org.junit.Test;

import junit.framework.TestCase;

public class HttpLoaderTest extends TestCase
{
	@Test
	public void testQualifies ()
	{
		final HttpLoader hl = new HttpLoader ();
		assertTrue ( hl.qualifies ( "http://" ) );
		assertTrue ( hl.qualifies ( "https://" ) );
		assertFalse ( hl.qualifies ( "www" ) );
	}

	@Test
	public void testLoadResource ()
	{
		final HttpLoader hl = new HttpLoader ();
		try {
			String path = "file:///" + new File ( "src/test/resources/services.json" ).getAbsolutePath ();
			assertNotNull ( hl.loadResource ( path ) );
		} catch (IOException e) {
			fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}
}
