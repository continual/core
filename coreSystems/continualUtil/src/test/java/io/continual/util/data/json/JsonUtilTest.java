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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import io.continual.util.data.json.JsonUtil.StringArrayValueParser;
import io.continual.util.data.json.JsonVisitor.ArrayOfObjectVisitor;

public class JsonUtilTest
{
	@Test
	public void testReadStringArray ()
	{
		final JSONObject base = new JSONObject ()
			.put ( "str", "string" )
			.put ( "arr", new JSONArray ().put ( "one" ).put ( "two" ).put ( "three" ) )
		;

		List<String> result = JsonUtil.readStringArray ( base, "str" );
		assertEquals ( 1, result.size () );
		assertEquals ( "string", result.iterator ().next () );
		
		result = JsonUtil.readStringArray ( base, "arr" );
		assertEquals ( 3, result.size () );
		
		final Iterator<String> it = result.iterator ();
		assertEquals ( "one", it.next () );
		assertEquals ( "two", it.next () );
		assertEquals ( "three", it.next () );
	}

	private static class IntegerHolder
	{
		public IntegerHolder ( int i ) { fValue = i; }
		public int getAndIncr () { return fValue++; }
		private int fValue;
	}

	@Test
	public void testArraySort ()
	{
		final JSONArray a = new JSONArray ()
			.put ( new JSONObject ().put ( "foo", 9 ) )
			.put ( new JSONObject ().put ( "foo", 6 ) )
			.put ( new JSONObject ().put ( "foo", 4 ) )
			.put ( new JSONObject ().put ( "foo", 3 ) )
			.put ( new JSONObject ().put ( "foo", 1 ) )
			.put ( new JSONObject ().put ( "foo", 2 ) )
			.put ( new JSONObject ().put ( "foo", 7 ) )
			.put ( new JSONObject ().put ( "foo", 5 ) )
			.put ( new JSONObject ().put ( "foo", 8 ) )
		;

		JsonUtil.sortArrayOfObjects ( a, "foo" );

		final IntegerHolder ih = new IntegerHolder ( 1 );
		JsonVisitor.forEachObjectIn ( a, new ArrayOfObjectVisitor() {

			@Override
			public boolean visit ( JSONObject t ) throws JSONException
			{
				assertEquals ( ih.getAndIncr(), t.getInt ( "foo" ) );
				return false;
			}
		} );
	}

	@Test
	public void testArraySort2 ()
	{
		final String arrayData = "[\n"
			+ "  {\n"
			+ "    \"op\": 20,\n"
			+ "    \"system\": \"CIN-13DIGITAL-TEMP\",\n"
			+ "    \"qty\": 316.4375,\n"
			+ "    \"category\": \"WASTE\"\n"
			+ "  },\n"
			+ "  {\n"
			+ "    \"op\": 20,\n"
			+ "    \"system\": \"CIN-13DIGITAL-TEMP\",\n"
			+ "    \"qty\": 100,\n"
			+ "    \"category\": \"INK\"\n"
			+ "  },\n"
			+ "  {\n"
			+ "    \"op\": 20,\n"
			+ "    \"system\": \"CIN-13DIGITAL-TEMP\",\n"
			+ "    \"qty\": 25521.25,\n"
			+ "    \"category\": \"LABOR\"\n"
			+ "  },\n"
			+ "  {\n"
			+ "    \"op\": 20,\n"
			+ "    \"system\": \"CIN-13DIGITAL-TEMP\",\n"
			+ "    \"qty\": 100,\n"
			+ "    \"category\": \"MATERIAL\"\n"
			+ "  },\n"
			+ "  {\n"
			+ "    \"op\": 30,\n"
			+ "    \"system\": \"CIN-ABG\",\n"
			+ "    \"qty\": 266.5234375,\n"
			+ "    \"category\": \"WASTE\"\n"
			+ "  },\n"
			+ "  {\n"
			+ "    \"op\": 30,\n"
			+ "    \"system\": \"CIN-ABG\",\n"
			+ "    \"qty\": 25521.25,\n"
			+ "    \"category\": \"LABOR\"\n"
			+ "  },\n"
			+ "  {\n"
			+ "    \"op\": 40,\n"
			+ "    \"system\": \"CIN-SLITINSPECT\",\n"
			+ "    \"qty\": 86.609375,\n"
			+ "    \"category\": \"WASTE\"\n"
			+ "  },\n"
			+ "  {\n"
			+ "    \"op\": 40,\n"
			+ "    \"system\": \"CIN-SLITINSPECT\",\n"
			+ "    \"qty\": 25521.25,\n"
			+ "    \"category\": \"LABOR\"\n"
			+ "  },\n"
			+ "  {\n"
			+ "    \"op\": 10,\n"
			+ "    \"system\": \"CIN-STAGING\",\n"
			+ "    \"qty\": 0.25,\n"
			+ "    \"category\": \"LABOR\"\n"
			+ "  }\n"
			+ "]";
		
		final JSONArray a = new JSONArray ( new JSONTokener ( arrayData ) );

		JsonUtil.sortArrayOfObjects ( a, "op" );

		assertEquals ( 10, a.getJSONObject ( 0 ).getInt ( "op" ) );
	}

	@Test
	public void testOverlay ()
	{
		final JSONObject target = new JSONObject ()
			.put ( "foo", 123 )
		;

		final JSONObject overlay1 = new JSONObject ()
			.put ( "foo", new JSONObject () )
		;

		final JSONObject updated = JsonUtil.overlay ( target, overlay1 );
		assertTrue ( updated == target );
		assertEquals ( 0, updated.getJSONObject ( "foo" ).keySet ().size () );

		final JSONObject overlay2 = new JSONObject ()
			.put ( "foo", new JSONObject ()
				.put ( "bar", 123.0 )
				.put ( "monkey", new JSONObject ()
					.put ( "bars", 3 )
					.put ( "brains", 50 )
				)
			)
		;
		JsonUtil.overlay ( updated, overlay2 );

		final JSONObject overlay3 = new JSONObject ()
			.put ( "foo", new JSONObject ()
				.put ( "monkey", new JSONObject ()
					.put ( "bars", 4 )
				)
			)
		;
		JsonUtil.overlay ( updated, overlay3 );

		assertEquals ( 4, updated.getJSONObject ( "foo" ).getJSONObject ( "monkey" ).getInt ( "bars" ) );
	}

	@Test
	public void testDateUtil ()
	{
		final Date d = new Date ( 1669151673000L );
		final JSONObject obj = new JSONObject ();
		JsonUtil.writeDate ( obj, "date", d );
		assertEquals ( "2022-11-22", obj.getString ( "date" ) );

		final LocalDate e = JsonUtil.readDate ( obj, "date" );
		assertEquals ( 2022, e.getYear () );
		assertEquals ( Month.NOVEMBER, e.getMonth () );
		assertEquals ( 22, e.getDayOfMonth () );
	}
	
	final String json = "{'id':12, 'name':'arzu' }";
	final String jsonArray = "[12, 'arzu' ]";
	
	@Test
	public void readJsonObject_with_inputStream(){
		InputStream input = new ByteArrayInputStream(json.getBytes());
		JSONObject o = JsonUtil.readJsonObject( input );
		assertNotNull ( o );
		assertEquals ( 12, o.getInt("id") );
		assertEquals ( "arzu", o.getString ( "name" ) );
	}
	
	@Test
	public void readJsonObject_with_reader(){
		Reader input = new StringReader(json);
		JSONObject o =  JsonUtil.readJsonObject( input );
		assertNotNull ( o );
		assertEquals ( 12, o.getInt("id") );
		assertEquals ( "arzu", o.getString ( "name" ) );
	}
	
	@Test
	public void readJsonObject_with_string(){
		JSONObject o =  JsonUtil.readJsonObject( json );
		assertNotNull ( o );
		assertEquals ( 12, o.getInt("id") );
		assertEquals ( "arzu", o.getString ( "name" ) );
	}
	
	@Test
	public void readJsonArray_with_inputStream(){
		InputStream input = new ByteArrayInputStream(jsonArray.getBytes());
		JSONArray o = JsonUtil.readJsonArray( input );
		assertNotNull ( o );
		assertEquals ( 12, o.get(0) );
		assertEquals ( "arzu", o.get(1) );
	}
	
	@Test
	public void readJsonArray_with_reader(){
		Reader input = new StringReader(jsonArray);
		JSONArray o =  JsonUtil.readJsonArray( input );
		assertNotNull ( o );
		assertEquals ( 12, o.get(0) );
		assertEquals ( "arzu", o.get(1) );
	}
	
	@Test
	public void readJsonArray_with_string(){
		JSONArray o =  JsonUtil.readJsonArray( jsonArray );
		assertNotNull ( o );
		assertEquals ( 12, o.get(0) );
		assertEquals ( "arzu", o.get(1) );
	}
	
	@Test
	public void readJsonValue(){
		Object o =  JsonUtil.readJsonValue( jsonArray );
		assertEquals ( "[12,\"arzu\"]", o.toString() );		
	}
	
	@Test
	public void clone_jsonObject(){
		JSONObject o =  JsonUtil.readJsonObject( json );
		JSONObject o2 = JsonUtil.clone(o);
		assertNotNull ( o2 );
		assertEquals ( 12, o2.getInt("id") );
		assertEquals ( "arzu", o2.getString ( "name" ) );
	}
	
	@Test
	public void clone_jsonObject_null(){
		assertNull ( JsonUtil.clone( (JSONObject)null) );
	}
	
	@Test
	public void clone_jsonArray(){
		JSONArray o =  JsonUtil.readJsonArray( jsonArray );
		JSONArray o2 = JsonUtil.clone(o);
		assertNotNull ( o2 );
		assertEquals ( 12, o2.get(0) );
		assertEquals ( "arzu", o2.get(1) );
	}
	
	@Test
	public void clone_jsonArray_null(){
		assertNull ( JsonUtil.clone( (JSONArray)null) );
	}
	
	@Test
	public void cloneJsonValue_object_null(){
		assertNull ( JsonUtil.cloneJsonValue( (Object)null) );
	}
	
	@Test
	public void cloneJsonValue_jsonArray_null(){
		assertNull ( JsonUtil.cloneJsonValue( (JSONArray)null) );
	}
	
	@Test
	public void cloneJsonValue_jsonArray(){
		JSONArray o =  JsonUtil.readJsonArray( jsonArray );
		JSONArray cloned =  (JSONArray)JsonUtil.cloneJsonValue(o);
		assertNotNull ( cloned );
		assertEquals ( 12, cloned.get(0) );
		assertEquals ( "arzu", cloned.get(1) );
	}
	
	@Test
	public void overlay_target_null(){
		JSONObject o =  JsonUtil.readJsonObject( json );
		assertNull ( JsonUtil.overlay( null, o) );
	}
	
	@Test
	public void overlay_overlay_null(){
		JSONObject o =  JsonUtil.readJsonObject( json );
		JSONObject cloned =  JsonUtil.overlay( o, null) ;
		assertNotNull ( cloned );
		assertEquals ( 12, cloned.getInt("id") );
		assertEquals ( "arzu", cloned.getString ( "name" ) );
	}
	
	@Test
	public void overlay(){
		JSONObject target =  JsonUtil.readJsonObject( json );
		JSONObject overlay =  JsonUtil.readJsonObject( json );
		overlay.put("name", JSONObject.NULL);
		JSONObject cloned =  JsonUtil.overlay( target, overlay) ;
		assertNotNull ( cloned );
		assertEquals ( 12, cloned.getInt("id") );
	}
	
	@Test
	public void copyInto_null(){
		JSONObject src =  JsonUtil.readJsonObject( json );
		JSONObject dest =  JsonUtil.readJsonObject( json );		
		JsonUtil.copyInto( src, null) ;
		JsonUtil.copyInto( null, dest) ;
		assertNotNull ( src );
		assertNotNull ( dest );
	}
	
	@Test
	public void copyInto(){
		JSONObject src =  JsonUtil.readJsonObject( json );
		JSONObject dest =  new JSONObject();
		JsonUtil.copyInto( src, dest) ;
		assertNotNull ( dest );
		assertEquals ( 12, dest.getInt("id") );
		assertEquals ( "arzu", dest.getString ( "name" ) );
	}
	
	@Test
	public void putDefault(){
		JSONObject src =  JsonUtil.readJsonObject( json );
		JsonUtil.putDefault( src, "newField", 555) ;
		assertEquals ( 555, src.getInt("newField") );
	}
	
	@Test
	public void putDefault_existing(){
		JSONObject src =  JsonUtil.readJsonObject( json );
		JsonUtil.putDefault( src, "id", 777) ;
		assertEquals ( 12, src.getInt("id") );
	}
	
	@Test
	public void getIndexOfStringInArray_null(){
		JSONArray arrayObject = JsonUtil.readJsonArray( jsonArray );
		int result =  JsonUtil.getIndexOfStringInArray( null, arrayObject );
		assertEquals ( -1, result );
		
		int result2 =  JsonUtil.getIndexOfStringInArray( "test", null );
		assertEquals ( -1, result2 );
	}
	
	@Test
	public void getIndexOfStringInArray(){
		JSONArray arrayObject = new JSONArray();
		arrayObject.put("test1");
		arrayObject.put("test2");
		arrayObject.put("test3");
		int result =  JsonUtil.getIndexOfStringInArray( "test2", arrayObject );
		assertEquals ( 1, result );		
	}
	
	@Test
	public void ensureStringInArray_null(){
		JSONArray arrayObject = JsonUtil.readJsonArray( jsonArray );
		boolean result =  JsonUtil.ensureStringInArray( null, arrayObject );
		assertEquals ( false, result );
		
		boolean result2 =  JsonUtil.ensureStringInArray( "test", null );
		assertEquals ( false, result2 );
	}
	
	@Test
	public void ensureStringInArray(){
		JSONArray arrayObject = new JSONArray();
		arrayObject.put("test1");
		arrayObject.put("test2");
		arrayObject.put("test3");
		boolean result =  JsonUtil.ensureStringInArray( "test10", arrayObject );
		assertEquals ( true, result );
		
		boolean result2 =  JsonUtil.ensureStringInArray( "test10", arrayObject );
		assertEquals ( false, result2 );
	}
	
	@Test
	public void removeStringFromArray_null(){
		JSONArray arrayObject = JsonUtil.readJsonArray( jsonArray );
		boolean result =  JsonUtil.removeStringFromArray( arrayObject, null );
		assertEquals ( false, result );
		
		boolean result2 =  JsonUtil.removeStringFromArray( null , "test");
		assertEquals ( false, result2 );
	}
	
	@Test
	public void removeStringFromArray(){
		JSONArray arrayObject = new JSONArray();
		arrayObject.put("test1");
		arrayObject.put("test2");
		arrayObject.put("test3");
		boolean result =  JsonUtil.removeStringFromArray( arrayObject , "test1");
		assertEquals ( true, result );
		
		boolean result2 =  JsonUtil.removeStringFromArray( arrayObject , "test1" );
		assertEquals ( false, result2 );
	}
	
	@Test
	public void resolveRef_null(){
		JSONObject topLevel =  JsonUtil.readJsonObject( json );
		JSONObject result =  JsonUtil.resolveRef( topLevel, null );
		assertEquals ( null, result );
	}
	
	@Test
	public void resolveRef_null_ref(){
		JSONObject local =  JsonUtil.readJsonObject( json );		
		JSONObject result =  JsonUtil.resolveRef( null, local );
		assertEquals ( local, result );
	}
	
	@Test(expected = JSONException.class)
	public void resolveRef_exception(){
		JSONObject local =  JsonUtil.readJsonObject( json );	
		local.put("$ref", "");
		JsonUtil.resolveRef( null, local );
	}
	
	@Test
	public void resolveRef_not_existing_field(){
		JSONObject topLevel =  JsonUtil.readJsonObject( json );	
		JSONObject local = new JSONObject();
		local.put("$ref", "#/fieldX");
		assertEquals ( null, JsonUtil.resolveRef( topLevel, local ) );		
	}
	
	@Test
	public void resolveRef(){
		JSONObject topLevel =  JsonUtil.readJsonObject( json );	
		topLevel.put("address",  JsonUtil.readJsonObject( "{\"str\": \"test street\"}" ));
		JSONObject local = new JSONObject();
		local.put("$ref", "#/address");
		JSONObject result = JsonUtil.resolveRef( topLevel, local );
		assertEquals ( "test street", result.getString ( "str" ) );
	}
	
	@Test
	public void writeConsistently(){
		JSONObject object =  JsonUtil.readJsonObject( json );
		assertEquals (
				  "{\n"
				+ "id:12,\n"
				+ "name:\"arzu\"\n"
				+ "}\n", 
				JsonUtil.writeConsistently( (Object)object ) );
	}
	
	@Test
	public void writeConsistently_array(){
		JSONArray object =  JsonUtil.readJsonArray( jsonArray );
		assertEquals (
				  "[\n"
				  + "12,\n"
				  + "\"arzu\"\n"
				  + "]\n", 
				JsonUtil.writeConsistently( (Object)object ) );
	}
	
	@Test
	public void hash(){
		JSONObject object =  JsonUtil.readJsonObject( json );
		assertEquals (191683747, JsonUtil.hash(object) );
	}
	
	@Test
	public void writeWithKeyOrder(){
		JSONObject object =  JsonUtil.readJsonObject( json );
		assertEquals (
				  "{\n"
				+ "    \"id\": 12,\n"
				+ "    \"name\": \"arzu\"\n"
				+ "}", 
				JsonUtil.writeWithKeyOrder( (Object)object ) );
	}
	
	@Test
	public void writeWithKeyOrder_array(){
		JSONArray object =  JsonUtil.readJsonArray( jsonArray );
		assertEquals (
				  "[\n"
				+ "    12,\n"
				+ "    \"arzu\"\n"
				+ "]", 
				JsonUtil.writeWithKeyOrder( (Object)object ) );
	}
	
	@Test
	public void writeWithKeyOrder_jsonArray(){
		JSONArray arrayObject = new JSONArray();
		arrayObject.put("fieldX");
		arrayObject.put("fieldY");		
		
		JSONArray object =  JsonUtil.readJsonArray( jsonArray );
		object.put(arrayObject);
		
		assertEquals (
				  "[\n"
				+ "    12,\n"
				+ "    \"arzu\",\n"
				+ "    [\n"
				+ "        \"fieldX\",\n"
				+ "        \"fieldY\"\n"
				+ "    ]\n"
				+ "]", 
				JsonUtil.writeWithKeyOrder( (JSONArray)object ) );
	}
	
	@Test
	public void writeWithKeyOrder_jsonObject(){
		JSONArray arrayObject = new JSONArray();
		arrayObject.put("name");
		arrayObject.put("id");
		
		JSONObject object =  JsonUtil.readJsonObject( json );
		object.put("__keyOrder", arrayObject);
		assertEquals (
				  "{\n"
				+ "    \"name\": \"arzu\",\n"
				+ "    \"id\": 12\n"
				+ "}", 
				JsonUtil.writeWithKeyOrder( (JSONObject)object ) );
	}
	
	@Test
	public void writeWithKeyOrder_jsonObject_inner(){
		JSONArray arrayObject = new JSONArray();
		arrayObject.put("name");
		arrayObject.put("address");
		arrayObject.put("id");
		arrayObject.put("non_existing_field");
		
		JSONObject innerObject = new JSONObject();
		innerObject.put("street", "our nice street");
		innerObject.put("city", "Ankara");
		
		JSONObject object =  JsonUtil.readJsonObject( json );
		object.put("address", innerObject);
		object.put("__keyOrder", arrayObject);
		assertEquals (
				  "{\n"
				+ "    \"name\": \"arzu\",\n"
				+ "    \"address\": {\n"
				+ "        \"city\": \"Ankara\",\n"
				+ "        \"street\": \"our nice street\"\n"
				+ "    },\n"
				+ "    \"id\": 12\n"
				+ "}", 
				JsonUtil.writeWithKeyOrder( (JSONObject)object ) );
	}
	
	@Test
	public void writeDateTime(){
		LocalDate today = LocalDate.now();
		JSONObject src =  JsonUtil.readJsonObject( json );
		src.put("myDate", "");
		Date d = new Date();
		JsonUtil.writeDateTime( src, "myDate", d ) ;
		assertEquals ( today, LocalDate.parse(src.getString("myDate")) );
	}
	
	@Test
	public void writeDateTime_localdate(){
		LocalDate today = LocalDate.now();
		
		LocalDate d = LocalDate.now();
		
		assertEquals ( today.toString(), JsonUtil.writeDate( d ).toString() );
	}
	
	@Test
	public void readDate_null(){		
		JSONObject base =  JsonUtil.readJsonObject( json );
		base.put("myDate", "");		
		assertEquals ( null, JsonUtil.readDate( base , "fieldX") );
	}
	
	@Test
	public void readDate_long(){		
		JSONObject base =  JsonUtil.readJsonObject( json );
		base.put("myDate", 1234444534545L);		
		assertEquals ( LocalDate.parse("2009-02-12"), JsonUtil.readDate( base , "myDate") );
	}
	
	@Test(expected = JSONException.class)
	public void readDate_unrecognized_format(){		
		JSONObject base =  JsonUtil.readJsonObject( json );
		base.put("myDate", "20220913");		
		assertEquals ( LocalDate.parse("2022-09-13"), JsonUtil.readDate( base , "myDate") );
	}
	
	@Test
	public void readDate_localDateTime(){		
		JSONObject base =  JsonUtil.readJsonObject( json );
		base.put("myDate", "2022-07-23T10:15:30");		
		assertEquals ( LocalDate.parse("2022-07-23"), JsonUtil.readDate( base , "myDate") );
	}
	
	@Test
	public void sortArrayOfObjects(){
		JSONArray o =  new JSONArray();
		o.put(new JSONObject().put ( "id", 90L ));
		o.put(new JSONObject().put ( "id", -50 ));
		o.put(new JSONObject().put ( "id", JSONObject.NULL ));
		o.put(new JSONObject().put ( "id", 10L ));		
		JsonUtil.sortArrayOfObjects( o , "id", true);		
		assertEquals (90L, ((JSONObject)o.get(0)).get("id") );
		assertEquals (10L, ((JSONObject)o.get(1)).get("id") );
		assertEquals (-50, ((JSONObject)o.get(2)).get("id") );
	}
	
	@Test(expected = JSONException.class)
	public void readStringArray_null(){
		JSONObject base =  JsonUtil.readJsonObject( json );		
		JsonUtil.readStringArray( base , null, null);
	}
	
	@Test
	public void readStringArray(){
		JSONObject base =  JsonUtil.readJsonObject( json );
		StringArrayValueParser parser = new StringArrayValueParser() {
			
			@Override
			public Collection<String> parse(String rawValue) {
				return Arrays.asList(rawValue, "***");
			}
		};
		List<String> result = JsonUtil.readStringArray( base , "id", parser);
		assertEquals("12", result.get(0));
		assertEquals("***", result.get(1));
	}
	
	
	
}
