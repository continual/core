package io.continual.iam.access;

import org.junit.Test;

import junit.framework.TestCase;

public class SimpleResourceTest extends TestCase
{
	@Test
	public void testConstructor ()
	{
		final SimpleResource sr = new SimpleResource ( "resid" );
		assertNotNull ( sr );
	}

	@Test
	public void testGetId ()
	{
		final String expect = "resid";
		final SimpleResource sr = new SimpleResource ( expect );
		assertEquals ( expect , sr.getId() );
	}

	@Test
	public void testToString ()
	{
		final String expect = "resid";
		final SimpleResource sr = new SimpleResource ( expect );
		assertEquals ( expect , sr.toString() );		
	}
}
