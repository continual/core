package io.continual.services;

import org.json.JSONObject;
import org.junit.Test;

import junit.framework.TestCase;

public class ServiceConfigTest extends TestCase
{
	@Test
	public void testRead ()
	{
		final JSONObject jo = new JSONObject ();
		jo.put ( "classname" , "io.continual.services.SimpleService" );
		final ServiceConfig sc = ServiceConfig.read ( jo );

		assertTrue ( sc.toString ().contains ( "classname" ) );
	}

	@Test
	public void testSetBaseConfig ()
	{
		final JSONObject jo = new JSONObject ();
		jo.put ( "classname" , "io.continual.services.SimpleService" );

		final ServiceConfig sc = ServiceConfig.read ( jo );
		sc.setBaseConfig ( new ConfigObject ( jo ) );

		assertTrue ( sc.toString ().contains ( "SimpleService" ) );
	}

	@Test
	public void testGetClassname1 ()
	{
		final JSONObject jo = new JSONObject ();
		jo.put ( "classname" , "io.continual.services.SimpleService" );

		final ServiceConfig sc = ServiceConfig.read ( jo );

		assertTrue ( sc.getClassname ().contains ( "SimpleService" ) );
	}

	@Test
	public void testGetClassname2 ()
	{
		final JSONObject jo = new JSONObject ();
		jo.put ( "class" , "io.continual.services.SimpleService" );

		final ServiceConfig sc = ServiceConfig.read ( jo );

		assertTrue ( sc.getClassname ().contains ( "SimpleService" ) );
	}

	@Test
	public void testGetName ()
	{
		final JSONObject jo = new JSONObject ();
		jo.put ( "name" , "service" );

		final ServiceConfig sc = ServiceConfig.read ( jo );

		assertEquals ( "service" , sc.getName () );
	}

	@Test
	public void testEnabled ()
	{
		final JSONObject jo = new JSONObject ();
		jo.put ( "enabled" , false );

		final ServiceConfig sc = ServiceConfig.read ( jo );

		assertFalse ( sc.enabled () );
	}

	@Test
	public void testOverwrite ()
	{
		final JSONObject jo = new JSONObject ();
		jo.put ( "name" , "service" );
		final ServiceConfig sc = ServiceConfig.read ( jo );

		sc.overwrite( new ProfileConfig ( new JSONObject () ) );

		assertTrue ( sc.toString ().contains ( "service" ) );
	}
}
