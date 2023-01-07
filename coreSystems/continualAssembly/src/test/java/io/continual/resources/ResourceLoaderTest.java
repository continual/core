package io.continual.resources;

import java.io.IOException;

import org.junit.Test;

import io.continual.resources.sources.AwsS3UriLoader;
import junit.framework.TestCase;

public class ResourceLoaderTest extends TestCase
{
	@Test
	public void testLoad ()
	{
		try {
			assertNull ( ResourceLoader.load ( null ) );
			assertNotNull ( ResourceLoader.load ( "services.json" ) );
		} catch (IOException e) {
			fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}

	@Test
	public void testToString ()
	{
		try {
			final ResourceLoader rl = new ResourceLoader ()
					.named ( "services" )
					.usingStandardSources ( false , AwsS3UriLoader.class );
			assertNull ( rl.load () );
			assertEquals ( "services" , rl.toString () );
		} catch (IOException e) {
			fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}		
	}
}
