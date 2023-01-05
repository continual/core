package io.continual.util.data.exprEval;

import org.json.JSONObject;
import org.junit.Test;

import junit.framework.TestCase;

public class JsonDataSourceTest extends TestCase
{
	@Test
	public void testEval1 ()
	{
		final JsonDataSource jds = new JsonDataSource ( new JSONObject ()
				.put ( "key", "${val1}${val2.val3}" ) );
		assertNull ( jds.eval ( "key2" ) );
		assertNotNull ( jds.eval ( "key" ) );
	}

	@Test
	public void testEval2 ()
	{
		assertNull ( new JsonDataSource ( null ).eval ( "key" ) );
	}
}
