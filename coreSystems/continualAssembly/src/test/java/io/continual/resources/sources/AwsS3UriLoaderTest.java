package io.continual.resources.sources;

import org.junit.Test;

import junit.framework.TestCase;

public class AwsS3UriLoaderTest extends TestCase
{
	@Test
	public void testConstructor ()
	{
		assertNotNull ( new AwsS3UriLoader () );
	}

	@Test
	public void testQualifies ()
	{
		assertFalse ( new AwsS3UriLoader ().qualifies ( "services.json" ) );
	}
}
