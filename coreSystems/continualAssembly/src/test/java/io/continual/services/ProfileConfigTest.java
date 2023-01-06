package io.continual.services;

import org.json.JSONObject;
import org.junit.Test;

import junit.framework.TestCase;

public class ProfileConfigTest extends TestCase
{
	@Test
	public void testConstructor ()
	{
		assertNotNull ( new ProfileConfig ( null ) );
	}

	@Test
	public void testRead ()
	{
		final JSONObject jo = new JSONObject ();
		jo.put ( "foo", "bar" );
		final ProfileConfig pc = ProfileConfig.read ( jo );

		assertEquals ( "bar" , pc.get ( "foo" ) );
	}

	@Test
	public void testGetConfigOverridesFor ()
	{
		final JSONObject jo = new JSONObject ();
		jo.put ( "service", new JSONObject ().put ( "foo" , "bar" ) );
		final ProfileConfig pc = new ProfileConfig ( jo );

		assertEquals ( 1 , pc.getConfigOverridesFor ( "service" ).size () );
	}
}
