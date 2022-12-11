package io.continual.iam.exceptions;

import org.junit.Test;

import junit.framework.TestCase;

public class IamGroupDoesNotExistTest extends TestCase
{
	private final String expect = "IamGroupDoesNotExist";

	@Test
	public void testConstructorString ()
	{
		final IamGroupDoesNotExist iamgdne = new IamGroupDoesNotExist ( expect );
		assertEquals ( expect , iamgdne.getMessage() );
	}

	@Test
	public void testConstructorThrowable ()
	{
		final IamGroupDoesNotExist iamgdne = new IamGroupDoesNotExist ( new Throwable ( expect ) );
		assertTrue ( iamgdne.getMessage().contains( expect ) );
	}

	@Test
	public void testConstructorStringAndThrowable ()
	{
		final IamGroupDoesNotExist iamgdne = new IamGroupDoesNotExist ( expect , new Throwable ( expect ) );
		assertEquals ( expect , iamgdne.getMessage() );
	}
}
