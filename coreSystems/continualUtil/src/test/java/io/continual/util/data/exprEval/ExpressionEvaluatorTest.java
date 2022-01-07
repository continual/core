package io.continual.util.data.exprEval;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import junit.framework.TestCase;

public class ExpressionEvaluatorTest extends TestCase
{
	@Test
	public void testJsonObjectEval ()
	{
		final JSONObject in = new JSONObject ()
			.put ( "a", "${b}${c.a}" )
			.put ( "b", 1 )
			.put ( "c", new JSONObject ().put ( "a", 3 ) )
			.put ( "d", new JSONArray ().put ( "foo" ).put ("element ${b}").put ("element 2") )
		;
		final JSONObject out = ExpressionEvaluator.evaluateJsonObject ( in, new JsonDataSource ( in )  );

		assertEquals ( "13", out.getString ( "a" ) );
		assertEquals ( "element 1", out.getJSONArray ( "d" ).get ( 1 ) );
	}
}
