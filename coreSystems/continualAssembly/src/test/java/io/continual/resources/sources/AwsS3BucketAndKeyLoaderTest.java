package io.continual.resources.sources;

import org.junit.Test;

import junit.framework.TestCase;

public class AwsS3BucketAndKeyLoaderTest extends TestCase
{
	@Test
	public void testConstructor ()
	{
		assertNotNull ( new AwsS3BucketAndKeyLoader ( "bucket" ) );
	}

	@Test
	public void testQualifies ()
	{
		assertTrue ( new AwsS3BucketAndKeyLoader ( "bucket" ).qualifies ( "services.json" ) );
	}
}
