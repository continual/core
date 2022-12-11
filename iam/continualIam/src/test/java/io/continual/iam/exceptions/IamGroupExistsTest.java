package io.continual.iam.exceptions;

import org.junit.Test;

import junit.framework.TestCase;

public class IamGroupExistsTest extends TestCase
{
	private final String expect = "IamGroupExists";

	@Test
	public void testConstructorString ()
	{
		final IamGroupExists iamge = new IamGroupExists ( expect );
		assertEquals ( expect , iamge.getMessage() );
	}

	@Test
	public void testConstructorThrowable ()
	{
		final IamGroupExists iamge = new IamGroupExists ( new Throwable ( expect ) );
		assertTrue ( iamge.getMessage().contains( expect ) );
	}

	@Test
	public void testConstructorStringAndThrowable ()
	{
		final IamGroupExists iamge = new IamGroupExists ( expect , new Throwable ( expect ) );
		assertEquals ( expect , iamge.getMessage() );
	}
}
