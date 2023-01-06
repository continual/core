package io.continual.services;

import java.io.InputStreamReader;

import org.junit.Test;

import junit.framework.TestCase;

public class ServiceSetTest extends TestCase
{
	@Test
	public void testReadConfig ()
	{
		assertNotNull ( ServiceSet.readConfig ( new InputStreamReader ( 
			ServiceSet.class.getClassLoader ().getResourceAsStream ( "services.json" ) ) ) );
	}

	@Test
	public void testApplyProfile ()
	{
		final ServiceSet ss = ServiceSet.readConfig ( new InputStreamReader ( 
			ServiceSet.class.getClassLoader ().getResourceAsStream ( "services.json" ) ) );
		ss.applyProfile ( "profile_key" );	// Valid
		ss.applyProfile ( "invalid_prof" );	// Invalid
	}

	@Test
	public void testGetService ()
	{
		final ServiceSet ss = ServiceSet.readConfig ( new InputStreamReader ( 
			ServiceSet.class.getClassLoader ().getResourceAsStream ( "services.json" ) ) );
		assertNotNull ( ss.getService ( "SimpleService1" ) );
	}

	@Test
	public void testGetServices ()
	{
		final ServiceSet ss = ServiceSet.readConfig ( new InputStreamReader ( 
			ServiceSet.class.getClassLoader ().getResourceAsStream ( "services.json" ) ) );
		assertNotNull ( ss.getServices () );
		assertFalse ( ss.getServices ().isEmpty () );
	}
}
