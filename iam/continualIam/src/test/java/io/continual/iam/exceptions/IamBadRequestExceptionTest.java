package io.continual.iam.exceptions;

import org.junit.Test;

import junit.framework.TestCase;

public class IamBadRequestExceptionTest extends TestCase
{
	private final String expect = "IamBadRequestException";

	@Test
	public void testConstructorString ()
	{
		final IamBadRequestException iambre = new IamBadRequestException ( expect );
		assertEquals ( expect , iambre.getMessage() );
	}

	@Test
	public void testConstructorThrowable ()
	{
		final IamBadRequestException iambre = new IamBadRequestException ( new Throwable ( expect ) );
		assertTrue ( iambre.getMessage().contains( expect ) );
	}

	@Test
	public void testConstructorStringAndThrowable ()
	{
		final IamBadRequestException iambre = new IamBadRequestException ( expect , new Throwable ( expect ) );
		assertEquals ( expect , iambre.getMessage() );
	}
}
