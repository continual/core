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

	@Test
	public void testEvalWithDefaults ()
	{
		assertEquals ( "32", new ExpressionEvaluator().evaluateText ( "${foo|32}" ) );

		assertEquals ( "64", new ExpressionEvaluator (
			new JsonDataSource (
				new JSONObject().put ( "foo", 64 )
			)
		).evaluateText ( "${foo|32}" ) );
	}

	@Test
	public void testEvalWithDefaultsAndSpaceInEvalText ()
	{
		assertEquals ( "32", new ExpressionEvaluator().evaluateText ( "${foo | 32 }" ) );

		assertEquals ( "64", new ExpressionEvaluator (
			new JsonDataSource (
				new JSONObject().put ( "foo", 64 )
			)
		).evaluateText ( "${ foo| 32}" ) );
	}

	@Test
	public void testEvalWithSpaceInEvalText ()
	{
		assertEquals ( "64", new ExpressionEvaluator (
			new JsonDataSource (
				new JSONObject().put ( "foo", 64 )
			)
		).evaluateText ( "${ foo }" ) );
	}
}
