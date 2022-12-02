package io.continual.util.nv.impl;

import org.junit.Test;

import junit.framework.TestCase;

public class nvEnvPropertiesTest extends TestCase
{
	@Test
	public void testConstructor ()
	{
		final nvEnvProperties nvep = new nvEnvProperties ();
		assertNotNull( nvep );
	}
}
