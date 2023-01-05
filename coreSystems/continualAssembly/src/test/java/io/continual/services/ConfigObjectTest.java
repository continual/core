
package io.continual.services;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.json.JSONObject;
import org.junit.Test;

public class ConfigObjectTest 
{
	@Test
	public void testConstructFromObject () throws IOException
	{
		final JSONObject jo = new JSONObject ()
			.put ( "foo", "bar" )
		;
		final ConfigObject co = new ConfigObject ( jo );

		assertEquals ( "bar", co.get ( "foo" ) );
	}
}
