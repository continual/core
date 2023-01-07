package io.continual.resources.sources;

import java.io.IOException;

import org.junit.Test;

import junit.framework.TestCase;

public class ClassResourceLoaderTest extends TestCase
{
	@Test
	public void testConstructor ()
	{
		assertNotNull ( new ClassResourceLoader ( ClassResourceLoader.class ) );
	}

	@Test
	public void testLoadResource ()
	{
		try {
			assertNull ( new ClassResourceLoader ( ClassResourceLoader.class )
						.loadResource ( "services.json" ) );
		} catch (IOException e) {
			fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}
}
