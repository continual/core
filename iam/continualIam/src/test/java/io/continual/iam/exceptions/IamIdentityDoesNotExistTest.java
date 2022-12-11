package io.continual.iam.exceptions;

import org.junit.Test;

import junit.framework.TestCase;

public class IamIdentityDoesNotExistTest extends TestCase
{
	private final String expect = "IamIdentityDoesNotExist";

	@Test
	public void testConstructorString ()
	{
		final IamIdentityDoesNotExist iamidne = new IamIdentityDoesNotExist ( expect );
		assertEquals ( expect , iamidne.getMessage() );
	}

	@Test
	public void testConstructorThrowable ()
	{
		final IamIdentityDoesNotExist iamidne = new IamIdentityDoesNotExist ( new Throwable ( expect ) );
		assertTrue ( iamidne.getMessage().contains( expect ) );
	}

	@Test
	public void testConstructorStringAndThrowable ()
	{
		final IamIdentityDoesNotExist iamidne = new IamIdentityDoesNotExist ( expect , new Throwable ( expect ) );
		assertEquals ( expect , iamidne.getMessage() );
	}
}
