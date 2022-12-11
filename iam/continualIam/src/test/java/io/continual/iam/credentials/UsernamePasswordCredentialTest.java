package io.continual.iam.credentials;

import org.junit.Test;

import junit.framework.TestCase;

public class UsernamePasswordCredentialTest extends TestCase
{
	private final String username = "username" , password = "password";

	@Test
	public void testToString ()
	{
		final String expect = "User/Pwd for " + username;
		final UsernamePasswordCredential upc = new UsernamePasswordCredential ( username , password );
		assertEquals( expect , upc.toString() );
	}

	@Test
	public void testGetUsername ()
	{
		final UsernamePasswordCredential upc = new UsernamePasswordCredential ( username , password );
		assertEquals ( username , upc.getUsername() );
	}

	@Test
	public void testGetPassword ()
	{
		final UsernamePasswordCredential upc = new UsernamePasswordCredential ( username , password );
		assertEquals ( password , upc.getPassword() );
	}
}
