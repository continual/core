package io.continual.iam.access;

import org.junit.Test;

import junit.framework.TestCase;

public class AccessExceptionTest extends TestCase
{
	@Test
	public void testConstructorString ()
	{
		final String expect = "AccessException";
		final AccessException ae = new AccessException ( expect );
		assertEquals ( expect , ae.getMessage() );
	}

	@Test
	public void testConstructorThrowable ()
	{
		final String expect = "AccessException";
		final AccessException ae = new AccessException ( new Throwable ( expect ) );
		assertTrue ( ae.getMessage().contains( expect ) );		
	}
}
