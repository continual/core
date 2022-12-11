package io.continual.iam.credentials;

import org.junit.Test;

import junit.framework.TestCase;

public class ApiKeyCredentialTest extends TestCase
{
	@Test
	public void testGetApiKey ()
	{
		final String expect = "apikey";
		ApiKeyCredential akc = new ApiKeyCredential ( expect , null , null );
		assertEquals ( expect , akc.getApiKey() );
	}

	@Test
	public void testGetContent ()
	{
		final String expect = "content";
		ApiKeyCredential akc = new ApiKeyCredential ( null , expect , null );
		assertEquals ( expect , akc.getContent() );
	}

	@Test
	public void testGetSignature ()
	{
		final String expect = "signature";
		ApiKeyCredential akc = new ApiKeyCredential ( null , null , expect );
		assertEquals ( expect , akc.getSignature() );
	}
}
