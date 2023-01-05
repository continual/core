package io.continual.util.data.exprEval;

import java.util.HashMap;
import java.util.Map;

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

	@Test
	public void testEvaluateSymbol ()
	{
		final Map<String, String> keyVal = new HashMap<String, String> ();
		final ExpressionEvaluator ee = new ExpressionEvaluator ( new MapDataSource ( keyVal ) ); 
		assertNull ( ee.evaluateSymbol ( "$foo" ) );

		keyVal.put ( "$foo", "$val" );
		assertEquals ( "$val", ee.evaluateSymbol ( "$foo" ) );

		keyVal.put ( "$null", null );
		assertNull ( ee.evaluateSymbol ( "$null" ) );
	}

	@Test
	public void testEvaluateTextToInt ()
	{
		final ExpressionEvaluator ee = new ExpressionEvaluator ( ( ExprDataSource[] ) null );
		assertEquals ( 0 , ee.evaluateTextToInt ( null , 0 ) );
		assertEquals ( 64 , ee.evaluateTextToInt ( "64" , 0 ) );
		assertEquals ( 1024 , ee.evaluateTextToInt ( 1024 , 0 ) );
	}

	@Test
	public void testEvaluateTextToLong ()
	{
		final ExpressionEvaluator ee = new ExpressionEvaluator ( ( ExprDataSource[] ) null );
		assertEquals ( 0L , ee.evaluateTextToLong ( null , 0L ) );
		assertEquals ( 64L , ee.evaluateTextToLong ( "64" , 0L ) );
		assertEquals ( 1024L , ee.evaluateTextToLong ( 1024L , 0L ) );
		assertEquals ( 1024 , ee.evaluateTextToLong ( 1024 , 0L ) );
	}

	@Test
	public void testEvaluateTextToBoolean ()
	{
		final ExpressionEvaluator ee = new ExpressionEvaluator ( ( ExprDataSource[] ) null );
		assertTrue ( ee.evaluateTextToBoolean ( null , true ) );
		assertTrue ( ee.evaluateTextToBoolean ( "true" , false ) );
		assertTrue ( ee.evaluateTextToBoolean ( true , false ) );
	}

	@Test
	public void testEvaluateJsonObject ()
	{
		final JSONObject in = new JSONObject ()
			.put ( "key", "val" )
		;
		final ExpressionEvaluator ee = new ExpressionEvaluator ( new JsonDataSource ( in ) );
		assertNull ( ee.evaluateJsonObject ( null ) );

		final JSONObject out = ee.evaluateJsonObject ( in );
		assertEquals ( "val", out.getString ( "key" ) );
	}


	@Test
	public void testEvaluateJsonArray ()
	{
		final JSONObject in = new JSONObject ()
			.put ( "key", new JSONArray ().put ( "foo" )
						.put ( new JSONObject ().put ( "inKey" , "inval" ) )
						.put ( new JSONArray ().put ( "1" ) )
						.put ( true ) )
		;
		final ExpressionEvaluator ee = new ExpressionEvaluator ( new JsonDataSource ( in ) );
		assertNull ( ee.evaluateJsonArray ( null ) );

		assertNotNull ( ee.evaluateJsonArray ( new JSONArray ().put ( "foo" ) ) );
		assertNotNull ( ee.evaluateJsonArray ( new JSONArray ().put ( new JSONObject ().put ( "inKey" , "inval" ) ) ) );
		assertNotNull ( ee.evaluateJsonArray ( new JSONArray ().put ( new JSONArray ().put ( "1" ) ) ) );
		assertNotNull ( ee.evaluateJsonArray ( new JSONArray ().put ( true ) ) );
	}

	@Test
	public void testEvaluateTextToDouble ()
	{
		assertEquals ( 0.0 , ExpressionEvaluator.evaluateTextToDouble ( null , 0.0 , ( ExprDataSource[] ) null ) );
		assertEquals ( 64.0 , ExpressionEvaluator.evaluateTextToDouble ( "64.0" , 0.0 , ( ExprDataSource[] ) null ) );
		assertEquals ( 1024.0 , ExpressionEvaluator.evaluateTextToDouble ( 1024.0 , 0.0 , ( ExprDataSource[] ) null ) );
		assertEquals ( 1024.0 , ExpressionEvaluator.evaluateTextToDouble ( 1024.0F , 0.0 , ( ExprDataSource[] ) null ) );
	}


	@Test
	public void testEvaluateText ()
	{
		assertNull ( new ExpressionEvaluator().evaluateText ( null ) );
		assertEquals ( "32", new ExpressionEvaluator().evaluateText ( "${foo|32}" ) );
		assertEquals ( "${32", new ExpressionEvaluator().evaluateText ( "${32" ) );	// No Close
	}
}
