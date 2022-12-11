package io.continual.iam.exceptions;

import org.junit.Test;

import junit.framework.TestCase;

public class IamIdentityExistsTest extends TestCase
{
	private final String expect = "IamIdentityExists";

	@Test
	public void testConstructorString ()
	{
		final IamIdentityExists iamie = new IamIdentityExists ( expect );
		assertEquals ( expect , iamie.getMessage() );
	}

	@Test
	public void testConstructorThrowable ()
	{
		final IamIdentityExists iamie = new IamIdentityExists ( new Throwable ( expect ) );
		assertTrue ( iamie.getMessage().contains( expect ) );
	}

	@Test
	public void testConstructorStringAndThrowable ()
	{
		final IamIdentityExists iamie = new IamIdentityExists ( expect , new Throwable ( expect ) );
		assertEquals ( expect , iamie.getMessage() );
	}
}
