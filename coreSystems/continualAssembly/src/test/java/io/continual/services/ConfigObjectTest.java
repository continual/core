
package io.continual.services;

import org.json.JSONObject;
import org.junit.Test;

import junit.framework.TestCase;

public class ConfigObjectTest extends TestCase
{
	@Test
	public void testConstructFromObject ()
	{
		final JSONObject jo = new JSONObject ()
			.put ( "foo", "bar" )
		;
		final ConfigObject co = new ConfigObject ( jo );

		assertEquals ( "bar", co.get ( "foo" ) );
		assertNull ( new ConfigObject ( null ).get ( "foo" ) );
	}

	@Test
	public void testConstructNoParams ()
	{
		final ConfigObject co = new ConfigObject ();

		assertNull ( co.get ( "foo" ) );
	}

	@Test
	public void testRead ()
	{
		final JSONObject jo = new JSONObject ();
		jo.put ( "foo", "bar" );
		final ConfigObject co = ConfigObject.read ( jo );

		assertEquals ( "bar" , co.get ( "foo" ) );
	}

	@Test
	public void testToString ()
	{
		final JSONObject jo = new JSONObject ();
		jo.put ( "foo" , "bar" );
		final ConfigObject co = ConfigObject.read ( jo );

		assertTrue ( co.toString ().contains ( "foo" ) );
		assertTrue ( co.toString ().contains ( "bar" ) );
	}

	@Test
	public void testSetBaseConfig ()
	{
		final JSONObject jo = new JSONObject ();
		jo.put ( "foo" , "bar" );
		final ConfigObject coBase = new ConfigObject ( jo );

		final ConfigObject co = new ConfigObject ();
		co.setBaseConfig ( coBase );

		assertEquals ( jo.length () , co.size () );
	}

	@Test
	public void testGetBoolean ()
	{
		final JSONObject jo = new JSONObject ();
		jo.put ( "foo" , true );
		final ConfigObject co = new ConfigObject ( jo );

		assertTrue ( co.getBoolean ( "foo" ) );
	}

	@Test
	public void testGetBooleanDefault ()
	{
		final ConfigObject co = new ConfigObject ();

		assertTrue ( co.getBoolean ( "foo" , true ) );
	}

	@Test
	public void testGetAllKeys ()
	{
		final ConfigObject co = new ConfigObject ();
		co.set ( "foo" , "bar" );

		assertNotNull ( co.getAllKeys () );
		assertEquals ( co.size () , co.getAllKeys ().size () );
	}

	@Test
	public void testGetSubConfigNoService ()
	{
		final ConfigObject co = new ConfigObject ();

		assertNotNull ( co.getSubConfig ( "service" ) );
		assertEquals ( 0 , co.getSubConfig ( "service" ).size () );
	}

	@Test
	public void testGetSubConfigWithService ()
	{
		final JSONObject jo = new JSONObject ();
		jo.put ( "service", new JSONObject ().put ( "foo" , "bar" ) );
		final ConfigObject co = new ConfigObject ( jo );

		assertEquals ( 1 , co.getSubConfig ( "service" ).size () );
	}
}
