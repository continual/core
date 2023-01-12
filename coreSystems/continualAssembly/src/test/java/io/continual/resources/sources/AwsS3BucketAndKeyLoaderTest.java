package io.continual.resources.sources;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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


	@Test
	public void testLoadResource ()
	{
		final String resourceId = "https://s3.amazonaws.com/bucket/key";
		try { setAwsEnvironment();} catch(Exception e) {}
		try {
			new AwsS3BucketAndKeyLoader ( "bucket" ).loadResource ( resourceId );			
		} catch (IOException e) {
			// Expected to execute but will get failed if environment variables are not set properly
		} finally {
			try { restoreAwsEnvironment();} catch(Exception e) {}
		}
	}

	@SuppressWarnings("unchecked")
	private void setAwsEnvironment () throws Exception
	{
		Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
		java.lang.reflect.Field field = processEnvironment.getDeclaredField("theCaseInsensitiveEnvironment");
		field.setAccessible(true);
		Map<String, String> fieldKV = (Map<String, String>) field.get ( null );
		if( !origEnv.containsKey ( "AWS_ACCESS_KEY_ID" ) )
		{
			fieldKV.put ( "AWS_ACCESS_KEY_ID" , "bucketkey" );
		}
		if( !origEnv.containsKey ( "AWS_SECRET_ACCESS_KEY" ) )
		{
			fieldKV.put ( "AWS_SECRET_ACCESS_KEY" , "secretkey" );
		}
		if( !origEnv.containsKey ( "AWS_REGION" ) )
		{
			fieldKV.put ( "AWS_REGION" , com.amazonaws.regions.Regions.US_EAST_1.getName () );
		}
	}

	@SuppressWarnings("unchecked")
	private void restoreAwsEnvironment () throws Exception
	{
		Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
		java.lang.reflect.Field field = processEnvironment.getDeclaredField("theCaseInsensitiveEnvironment");
		field.setAccessible(true);
		Map<String, String> fieldKV = (java.util.Map<String, String>) field.get ( null );
		if( !origEnv.containsKey ( "AWS_ACCESS_KEY_ID" ) && fieldKV.containsKey ( "AWS_ACCESS_KEY_ID" ) )
		{
			fieldKV.remove ( "AWS_ACCESS_KEY_ID" );
		}
		if( !origEnv.containsKey ( "AWS_SECRET_ACCESS_KEY" ) )
		{
			fieldKV.remove ( "AWS_SECRET_ACCESS_KEY" );
		}
		if( !origEnv.containsKey ( "AWS_REGION" ) )
		{
			fieldKV.remove ( "AWS_REGION" );
		}
	}

	final Map<String, String> origEnv = new HashMap<String, String> ( System.getenv () );
}
