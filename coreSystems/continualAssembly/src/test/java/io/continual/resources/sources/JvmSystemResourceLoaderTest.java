package io.continual.resources.sources;

import java.io.IOException;

import org.junit.Test;

import junit.framework.TestCase;

public class JvmSystemResourceLoaderTest extends TestCase
{
	@Test
	public void testQualifies ()
	{
		assertTrue ( new JvmSystemResourceLoader ().qualifies ( "services.json" ) );
	}

	@Test
	public void testLoadResource ()
	{
		try {
			assertNotNull ( new JvmSystemResourceLoader ().loadResource ( "services.json" ) );
		} catch (IOException e) {
			fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}
}
