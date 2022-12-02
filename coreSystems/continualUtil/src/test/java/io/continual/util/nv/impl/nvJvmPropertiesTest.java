package io.continual.util.nv.impl;

import org.junit.Test;

import junit.framework.TestCase;

public class nvJvmPropertiesTest extends TestCase
{
	@Test
	public void testConstructor ()
	{
		final nvJvmProperties nvjp = new nvJvmProperties ();
		assertNotNull( nvjp );
	}
}
