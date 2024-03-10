package io.continual.services.model.core.data;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import junit.framework.TestCase;

public class ModelDataAccessTest extends TestCase
{
	@Test
	public void testBooleanAccess ()
	{
		final JSONObject data = new JSONObject ()
			.put ( "b", true )
			.put ( "n1", 1 )
			.put ( "n2", 2.3 )
			.put ( "s", "strrrr" )
			.put ( "map", new JSONObject ().put ( "foo", "bar" ) )
			.put ( "arr", new JSONArray ().put ( "foo" ) )
		;

		final ModelDataObjectAccess mda = new JsonObjectAccess ( data );
		assertTrue ( mda.getBoolean ( "b" ) );
		assertEquals ( 1, mda.getNumber ( "n1" ) );
		assertEquals ( 2.3, mda.getNumber ( "n2" ) );
	}
}
