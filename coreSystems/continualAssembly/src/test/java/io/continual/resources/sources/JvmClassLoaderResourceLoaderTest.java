package io.continual.resources.sources;

import java.io.IOException;

import org.junit.Test;

import junit.framework.TestCase;

public class JvmClassLoaderResourceLoaderTest extends TestCase
{
	@Test
	public void testConstructor ()
	{
		assertNotNull ( new JvmClassLoaderResourceLoader ( JvmClassLoaderResourceLoader.class ) );
	}

	@Test
	public void testLoadResource ()
	{
		try {
			assertNotNull ( new JvmClassLoaderResourceLoader ( JvmClassLoaderResourceLoader.class )
							.loadResource ( "services.json" ) );
		} catch (IOException e) {
			fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}
}
