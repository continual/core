package io.continual.iam.exceptions;

import org.junit.Test;

import junit.framework.TestCase;

public class IamSvcExceptionTest extends TestCase
{
	private final String expect = "IamSvcException";

	@Test
	public void testConstructorString ()
	{
		final IamSvcException iamse = new IamSvcException ( expect );
		assertEquals ( expect , iamse.getMessage() );
	}

	@Test
	public void testConstructorThrowable ()
	{
		final IamSvcException iamse = new IamSvcException ( new Throwable ( expect ) );
		assertTrue ( iamse.getMessage().contains( expect ) );
	}

	@Test
	public void testConstructorStringAndThrowable ()
	{
		final IamSvcException iamse = new IamSvcException ( expect , new Throwable ( expect ) );
		assertEquals ( expect , iamse.getMessage() );
	}
}
