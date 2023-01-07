package io.continual.resources.sources;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import junit.framework.TestCase;

public class FileLoaderTest extends TestCase
{
	@Test
	public void testConstructor ()
	{
		assertNotNull ( new FileLoader () );
		assertNotNull ( new FileLoader ( new File ("src/test/resources/services.json" ) ) );
	}

	@Test
	public void testQualifies ()
	{
		assertTrue ( new FileLoader ().qualifies ( "src/test/resources/services.json" ) );
	}

	@Test
	public void testLoadResource ()
	{
		try {
			assertNotNull ( new FileLoader ().loadResource ( "src/test/resources/services.json" ) );
			assertNull ( new FileLoader ().loadResource ( "dummy.json" ) );
			assertNull ( new FileLoader ( new File ( "abc.txt" ) ).loadResource ( "services.json" ) );
		} catch (IOException e) {
			fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}
}
