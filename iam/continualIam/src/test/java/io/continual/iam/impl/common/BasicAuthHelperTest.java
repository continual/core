package io.continual.iam.impl.common;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import io.continual.iam.credentials.UsernamePasswordCredential;
import junit.framework.TestCase;

public class BasicAuthHelperTest extends TestCase
{
	// User Name - continual : Password - continual
	private final String authHeader = "Basic " + Base64.getEncoder ()
							.encodeToString ( "continual:continual".getBytes () );

	@Test
	public void testReadUsernamePasswordCredential_NoAuth ()
	{
		final HeaderReader hr = new TestHeaderReader ( "key" , "value" );
		assertNull ( BasicAuthHelper.readUsernamePasswordCredential ( hr ) );
	}

	@Test
	public void testReadUsernamePasswordCredential_Illegal1 ()
	{
		final HeaderReader hr = new TestHeaderReader ( BasicAuthHelper.kSetting_AuthHeader , 
				authHeader.split ( " " )[0] );
		assertNull ( BasicAuthHelper.readUsernamePasswordCredential ( hr ) );
	}

	@Test
	public void testReadUsernamePasswordCredential_Illegal2 ()
	{
		final HeaderReader hr = new TestHeaderReader ( BasicAuthHelper.kSetting_AuthHeader , 
				"Bearer " + authHeader.split ( " " )[1] );
		assertNull ( BasicAuthHelper.readUsernamePasswordCredential ( hr ) );
	}

	@Test
	public void testReadUsernamePasswordCredential_UsernameEmpty ()
	{
		final String noUserNameAuthHeader = "Basic " + Base64.getEncoder ()
							.encodeToString ( ":continual".getBytes () );
		final HeaderReader hr = new TestHeaderReader ( BasicAuthHelper.kSetting_AuthHeader , noUserNameAuthHeader );
		assertNull ( BasicAuthHelper.readUsernamePasswordCredential ( hr ) );
	}

	@Test
	public void testReadUsernamePasswordCredential_NoColon ()
	{
		final String invalidSeparatorAuthHeader = "Basic " + Base64.getEncoder ()
							.encodeToString ( "continual/continual".getBytes () );
		final HeaderReader hr = new TestHeaderReader ( BasicAuthHelper.kSetting_AuthHeader , invalidSeparatorAuthHeader );
		assertNull ( BasicAuthHelper.readUsernamePasswordCredential ( hr ) );
	}

	@Test
	public void testEmptyConstructor ()
	{
		assertNotNull ( new BasicAuthHelper () );
	}

	@Test
	public void testReadUsernamePasswordCredential_Valid ()
	{
		final String expect = "continual";
		final HeaderReader hr = new TestHeaderReader ( BasicAuthHelper.kSetting_AuthHeader , authHeader );
		final UsernamePasswordCredential upc = BasicAuthHelper.readUsernamePasswordCredential ( hr );
		assertNotNull ( upc );
		assertEquals ( expect , upc.getUsername() );
		assertEquals ( expect , upc.getPassword() );
	}

	private static class TestHeaderReader implements HeaderReader
	{
		Map<String , String> keyValuePair = new HashMap<>();
		public TestHeaderReader ( String key , String value )
		{
			keyValuePair.put ( key , value );
		}
		@Override
		public String getFirstHeader(String header) {
			return keyValuePair.get ( header );
		}
	}
}
