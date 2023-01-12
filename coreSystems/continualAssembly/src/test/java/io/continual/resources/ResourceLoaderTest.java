package io.continual.resources;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

	@Test
	public void testS3Available ()
	{
		try { setEnvironment();} catch(Exception e) {}
		ResourceLoader.s3Available ();			
		try { restoreEnvironment();} catch(Exception e) {}
	}

	@SuppressWarnings("unchecked")
	private void setEnvironment () throws Exception
	{
		Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
		java.lang.reflect.Field field = processEnvironment.getDeclaredField("theCaseInsensitiveEnvironment");
		field.setAccessible(true);
		Map<String, String> fieldKV = (Map<String, String>) field.get ( null );
		if( !origEnv.containsKey ( "DRIFT_ALLOW_S3" ) )
		{
			fieldKV.put ( "DRIFT_ALLOW_S3" , "true" );
		}
	}

	@SuppressWarnings("unchecked")
	private void restoreEnvironment () throws Exception
	{
		Class<?> processEnvironment = Class.forName("java.lang.ProcessEnvironment");
		java.lang.reflect.Field field = processEnvironment.getDeclaredField("theCaseInsensitiveEnvironment");
		field.setAccessible(true);
		Map<String, String> fieldKV = (java.util.Map<String, String>) field.get ( null );
		if( !origEnv.containsKey ( "DRIFT_ALLOW_S3" ) && fieldKV.containsKey ( "DRIFT_ALLOW_S3" ) )
		{
			fieldKV.remove ( "DRIFT_ALLOW_S3" );
		}
	}

	final Map<String, String> origEnv = new HashMap<String, String> ( System.getenv () );
}
