/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.util.data.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class JsonEvalTest
{
	
    private JSONObject generateJSONObject(String fieldValue) {
    	JSONObject object = new JSONObject();
    	object.put("id", fieldValue);
    	object.put("name", "testField");
    	object.put("isEmployee", true);
    	object.put("age", 40);
    	object.put("timestamp", 999999L);
    	object.put("salary", 100.32d);
    	
    	JSONObject innerObject = new JSONObject();
    	innerObject.put("street", "12 st");
    	object.put("address", innerObject);
    	return object;
    }
    
	@Test
	public void testSimpleEval ()
	{
		assertEquals ( "foo", JsonEval.eval (
			new JSONObject()
				.put ( "bar","foo" )
			,
			"bar" ) );
		assertNull ( JsonEval.eval (
			new JSONObject()
				.put ( "bar","foo" )
			,
			"ch" ) );
	}

	@Test
	public void testSubobjectEval ()
	{
		assertEquals ( "foo", JsonEval.eval (
			new JSONObject()
				.put ( "bar",
					new JSONObject ()
						.put ( "bee", "foo" )
				)
			,
			"bar.bee" ) );

		assertNull ( JsonEval.eval (
			new JSONObject()
				.put ( "bar",
					new JSONObject ()
						.put ( "bee", "foo" )
				)
			,
			"bar.beet" ) );
	}

	@Test
	public void testArrayEval ()
	{
		assertEquals ( "zugga", JsonEval.eval (
			new JSONObject()
				.put ( "bar",
					new JSONArray ()
						.put (
							new JSONObject ()
								.put ( "ugga", "mugga" )
						)
						.put (
							new JSONObject ()
								.put ( "sugga", "zugga" )
						)
				)
			,
			"bar[1].sugga" ) );
	}

	@Test
	public void testArrayResult ()
	{
		final Object o = JsonEval.eval (
			new JSONObject()
				.put ( "bar",
					new JSONArray ()
						.put (
							new JSONObject ()
								.put ( "ugga", "mugga" )
						)
						.put (
							new JSONObject ()
								.put ( "sugga", "zugga" )
						)
				)
			,
			"bar" );
		assertTrue ( o instanceof JSONArray );
		final JSONArray a = (JSONArray) o;
		assertEquals ( 2, a.length() );
	}

	@Test
	public void testSet ()
	{
		final JSONObject start = new JSONObject ();
		JsonEval.setValue ( start, "foo.bar.bee", 3 );
		assertEquals ( 3, JsonEval.eval ( start, "foo.bar.bee" ) );

		JsonEval.setValue ( start, "foo.bar.baz", new JSONObject ().put ( "carrot", "orange" ) );
		final JSONObject baz = start.getJSONObject ( "foo" ).getJSONObject ( "bar" ).getJSONObject ( "baz" );
		assertEquals ( "orange", baz.get ( "carrot" ));

		assertTrue ( JsonEval.hasKey ( start, "foo.bar.bee" ) );
		assertTrue ( JsonEval.hasKey ( start, "foo.bar.baz" ) );
		assertTrue ( JsonEval.hasKey ( start, "foo.bar.baz.carrot" ) );
		assertFalse ( JsonEval.hasKey ( start, "foo.bar.baz.beet" ) );
		assertFalse ( JsonEval.hasKey ( start, "foo.beast.baz.beet" ) );
	}

	@Test
	public void testArrayOfArray ()
	{
		final JSONObject start = new JSONObject ();
		JsonEval.setValue ( start, "foo.bar.bee", new JSONArray () );
		JsonEval.setValue ( start, "foo.bar.bee.bug", 5, true );

		assertEquals ( 5, JsonEval.eval ( start, "foo.bar.bee[0].bug" ) );
	}

	@Test
	public void testGetContainer ()
	{
		final JSONObject o = new JSONObject ()
			.put ( "a", new JSONObject ()
				.put ( "b", new JSONObject ()
					.put ( "c", 123 )
				)
			)
		;
		
		final JSONObject container = JsonEval.getContainer ( o, "a.b" );
		assertEquals ( 123, container.getInt ( "c" ));

		final JSONObject container2 = JsonEval.getContainerOf ( o, "a.b.c" );
		assertEquals ( 123, container2.getInt ( "c" ));
	}
	
	@Test
	public void eval_null(){
		JSONObject jsonObject = generateJSONObject("500");
		assertNull(JsonEval.eval( jsonObject, "text"));
	}
	
	@Test
	public void eval_default_value_negative(){
		JSONObject jsonObject = generateJSONObject("500");
		assertEquals("-1", JsonEval.eval( jsonObject, "text", "-1"));
	}
	
	@Test
	public void eval_default_value(){
		JSONObject jsonObject = generateJSONObject("500");
		assertEquals("500", JsonEval.eval( jsonObject, "id", "-1"));
	}
	
	@Test
	public void evalToString(){
		JSONObject jsonObject = generateJSONObject("500");
		assertEquals("500", JsonEval.evalToString( jsonObject, "id"));
	}
	
	@Test
	public void evalToString_negative(){
		JSONObject jsonObject = generateJSONObject("500");
		assertEquals("", JsonEval.evalToString( jsonObject, "xx"));
	}
	
	@Test
	public void evalToBoolean(){
		JSONObject jsonObject = generateJSONObject("");
		assertEquals(true, JsonEval.evalToBoolean( jsonObject, "isEmployee"));
	}
	
	@Test
	public void evalToBoolean_stringValue(){
		JSONObject jsonObject = generateJSONObject("yes");
		assertEquals(true, JsonEval.evalToBoolean( jsonObject, "id"));
	}
	
	@Test
	public void evalToBoolean_negative(){
		JSONObject jsonObject = generateJSONObject("yes");
		assertEquals(false, JsonEval.evalToBoolean( jsonObject, "xx"));
	}
	
	@Test
	public void evalToInt(){
		JSONObject jsonObject = generateJSONObject("test");
		assertEquals(40, JsonEval.evalToInt( jsonObject, "age", -1));
	}
	
	@Test
	public void evalToInt_stringValue(){
		JSONObject jsonObject = generateJSONObject("15");
		assertEquals(15, JsonEval.evalToInt( jsonObject, "id", -1));
	}
	
	@Test
	public void evalToInt_negative(){
		JSONObject jsonObject = generateJSONObject("15");
		assertEquals(-1, JsonEval.evalToInt( jsonObject, "xx", -1));
	}
	
	@Test
	public void evalToLong(){
		JSONObject jsonObject = generateJSONObject("test");
		assertEquals(999999, JsonEval.evalToLong( jsonObject, "timestamp", -1));
	}
	
	@Test
	public void evalToLong_stringValue(){
		JSONObject jsonObject = generateJSONObject("777");
		assertEquals(777, JsonEval.evalToLong( jsonObject, "id", -1));
	}
	
	@Test
	public void evalToLong_negative(){
		JSONObject jsonObject = generateJSONObject("15");
		assertEquals(-1, JsonEval.evalToLong( jsonObject, "xx", -1));
	}
	
	@Test
	public void evalToDouble(){
		JSONObject jsonObject = generateJSONObject("test");
		assertEquals(100.32, JsonEval.evalToDouble( jsonObject, "salary", -1) ,0);
	}
	
	@Test
	public void evalToDouble_stringValue(){
		JSONObject jsonObject = generateJSONObject("43.99");
		assertEquals(43.99, JsonEval.evalToDouble( jsonObject, "id", -1), 0);
	}
	
	@Test
	public void evalToDouble_negative(){
		JSONObject jsonObject = generateJSONObject("15");
		assertEquals(-1, JsonEval.evalToDouble( jsonObject, "xx", -1), 0);
	}
	
	@Test
	public void evalToObject(){
		JSONObject jsonObject = generateJSONObject("test");
		assertEquals("{\"street\":\"12 st\"}", JsonEval.evalToObject( jsonObject, "address").toString());
	}
	
	@Test
	public void evalToObject_negative(){
		JSONObject jsonObject = generateJSONObject("15");
		assertNotNull(JsonEval.evalToObject( jsonObject, "xx"));
		assertEquals("{}", JsonEval.evalToObject( jsonObject, "xx").toString());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void evalToObject_exception(){
		JSONObject jsonObject = generateJSONObject("15");
		jsonObject.put("tmp", new JSONArray());
		JsonEval.evalToObject( jsonObject, "tmp");
	}
	
	@Test
	public void evalToArray(){
		JSONObject jsonObject = generateJSONObject("test");
		JSONArray list = new JSONArray();
		list.put("arr-1");
		jsonObject.put("list", list );
		assertEquals("[\"arr-1\"]", JsonEval.evalToArray( jsonObject, "list").toString());
	}
	
	@Test
	public void evalToArray_negative(){
		JSONObject jsonObject = generateJSONObject("15");
		assertNotNull(JsonEval.evalToArray( jsonObject, "xx"));
		assertEquals("[]", JsonEval.evalToArray( jsonObject, "xx").toString());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void evalToArray_exception(){
		JSONObject jsonObject = generateJSONObject("test");
		JsonEval.evalToArray( jsonObject, "address");
	}
	
	@Test
	public void substitute(){
		JSONObject jsonObject = generateJSONObject("15");
		assertEquals("id", JsonEval.substitute( "id", jsonObject));
	}
	
	@Test
	public void hasKey(){
		JSONObject jsonObject = generateJSONObject("15");
		assertEquals(true, JsonEval.hasKey( jsonObject,  "id"));
	}
	
	@Test
	public void eval_dots(){
		JSONObject jsonObject = generateJSONObject("500");
		assertNull(JsonEval.eval( jsonObject, "address[0].street"));
	}
	
	@Test
	public void eval_wrong_array_index(){
		JSONObject jsonObject = generateJSONObject("500");
		jsonObject.put("array", new JSONArray().put("test"));
		assertNull(JsonEval.eval( jsonObject, "array[0z].field"));
	}
	
	@Test
	public void eval_wrong_format(){
		JSONObject jsonObject = generateJSONObject("500");
		jsonObject.put("array", new JSONArray().put("test"));
		assertNull(JsonEval.eval( jsonObject, "foo[0[1]]"));
	}
	
	
}
