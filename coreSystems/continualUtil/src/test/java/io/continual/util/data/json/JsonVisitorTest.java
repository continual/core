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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import io.continual.util.data.json.JsonVisitor.ArrayOfObjectVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayOfStringVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectFilter;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;
import io.continual.util.data.json.JsonVisitor.ValueReader;
import io.continual.util.nv.impl.nvJsonObject;

public class JsonVisitorTest{
	
	private ArrayVisitor<String, Exception> arrayVisitor = new ArrayVisitor<String, Exception>() {

		@Override
		public boolean visit(String t) throws JSONException, Exception {
			return true;
		}
		
	};
	
    private JSONArray generateArray(){
    	JSONArray arr = new JSONArray();
    	arr.put("test");
    	arr.put("con");
    	return arr;
    }
    
    private JSONArray generateArrayWithObjects() {
    	JSONArray arr = new JSONArray();
    	arr.put(generateJsonObject("900"));
    	arr.put(generateJsonObject("250"));
    	return arr;
    }
    
    private JSONObject generateJsonObject(String fieldValue) {
    	JSONObject object = new JSONObject();
    	object.put("id", fieldValue);
    	object.put("name", "testField");
    	return object;
    }
    
    @Test
    public void forEachElement_null() throws Exception {
    	JsonVisitor.forEachElement(null, arrayVisitor);
    }
	
    @Test
    public void forEachElement() throws Exception {
    	List<String> fieldsList = new ArrayList<>();
    	ArrayVisitor<String, Exception> arrayVisitor = new ArrayVisitor<String, Exception>() {

    		@Override
    		public boolean visit(String t) throws JSONException, Exception {
    			fieldsList.add(t);
    			return true;
    		}
    		
    	};
    	Assert.assertEquals(0, fieldsList.size());
    	JsonVisitor.forEachElement(generateArray(), arrayVisitor);
    	Assert.assertEquals(2, fieldsList.size());
    }
    
    @Test
    public void forEachElement_visit_false() throws Exception {
    	JSONArray array = generateArray();
    	array.put("test");
    	List<String> fieldsList = new ArrayList<>();
    	ArrayVisitor<String, Exception> arrayVisitor = new ArrayVisitor<String, Exception>() {

    		@Override
    		public boolean visit(String t) throws JSONException, Exception {
    			if(fieldsList.contains(t)) {
    				return false;
    			}else {
    				fieldsList.add(t);
    				return true;
    			}
    		}
    		
    	};
    	Assert.assertEquals(0, fieldsList.size());
    	JsonVisitor.forEachElement(array, arrayVisitor);
    	Assert.assertEquals(2, fieldsList.size());
    }
    
    @Test
    public void forEachStringElement() throws Exception {
    	JSONArray array = generateArray();
    	List<String> fieldsList = new ArrayList<>();
    	ArrayOfStringVisitor arrayVisitor = new ArrayOfStringVisitor() {

			@Override
			public boolean visit(String t) throws JSONException {
				if(t.startsWith("tes")) {
					fieldsList.add(t);
				}
				return true;
			}
    		
    	};
    	Assert.assertEquals(0, fieldsList.size());
    	JsonVisitor.forEachStringElement(array, arrayVisitor);
    	Assert.assertEquals(1, fieldsList.size());
    }
    
    @Test
    public void forEachObjectIn() throws Exception {
    	JSONArray array = generateArrayWithObjects();
    	List<JSONObject> fieldsList = new ArrayList<>();
    	ArrayOfObjectVisitor arrayVisitor = new ArrayOfObjectVisitor() {

			@Override
			public boolean visit(JSONObject t) throws JSONException {
				fieldsList.add(t);
				return true;
			}
    		
    	};
    	Assert.assertEquals(0, fieldsList.size());
    	JsonVisitor.forEachObjectIn(array, arrayVisitor);
    	Assert.assertEquals(2, fieldsList.size());
    }
    
    @Test
    public void objectToMap() {
    	JSONObject object = generateJsonObject("800");
    	HashMap<String, String> map = JsonVisitor.objectToMap(object);
    	Assert.assertNotNull(map);
    	Assert.assertEquals(2, map.size());
    	Assert.assertEquals("800", map.get("id"));
    	Assert.assertEquals("testField", map.get("name"));
    }
    
    @Test
    public void objectToMap_null() {
    	HashMap<String, String> map = JsonVisitor.objectToMap(null);
    	Assert.assertNotNull(map);
    	Assert.assertEquals(0, map.size());
    }
    
    @Test
    public void findMatchingObjects() {
    	JSONArray array = generateArrayWithObjects();
    	ObjectFilter filterIdField = new ObjectFilter() {

			@Override
			public boolean matches(JSONObject item) {
				return item.get("id").equals("250");
			}
    		
    	};
    	List<JSONObject> result = JsonVisitor.findMatchingObjects(array, filterIdField);
    	Assert.assertEquals(1, result.size());
    }
    
    @Test
    public void listContains_null() {
    	Assert.assertEquals(false, JsonVisitor.listContains(null, ""));
    }
    
    @Test
    public void listContains_object_null() {
    	JSONArray array = new JSONArray();
    	array.put( JSONObject.NULL);
    	Assert.assertEquals(false, JsonVisitor.listContains(array, ""));
    }
    
    @Test
    public void listContains() {
    	JSONArray array = generateArrayWithObjects();
    	Assert.assertEquals(true, JsonVisitor.listContains(array, "{\"name\":\"testField\",\"id\":\"900\"}"));
    }
    
    @Test
    public void listContains_not_found() {
    	JSONArray array = generateArrayWithObjects();
    	Assert.assertEquals(false, JsonVisitor.listContains(array, "{}"));
    }
    
    @Test
    public void arrayToList() throws Exception {
    	JSONArray array = generateArray();
    	ValueReader<String, String> addStarToValue = new ValueReader<String, String>() {

			@Override
			public String read(String val) {
				return val + "*";
			}

    	};
    	List<String> result = JsonVisitor.arrayToList(array, addStarToValue);
    	Assert.assertEquals(2, result.size());
    	Assert.assertEquals("test*", result.get(0));
    	Assert.assertEquals("con*", result.get(1));
    }
    
    @Test
    public void arrayToList_null() throws Exception {
    	ValueReader<String, String> addStarToValue = new ValueReader<String, String>() {

			@Override
			public String read(String val) {
				return val + "*";
			}

    	};
    	List<String> result = JsonVisitor.arrayToList(null, addStarToValue);
    	Assert.assertEquals(0, result.size());
    }
    
    @Test
    public void arrayToList2_null() {
    	Assert.assertEquals(0, JsonVisitor.arrayToList(null).size());
    }
    
    @Test
    public void arrayToList2() {
    	JSONArray array = generateArray();
    	List<String> result = JsonVisitor.arrayToList(array);
    	Assert.assertNotNull(result);
    	Assert.assertEquals(2, result.size());
    	Assert.assertEquals("test", result.get(0));
    	Assert.assertEquals("con", result.get(1));
    }
    
    @Test
    public void arrayToIntList() {
    	JSONArray array = new JSONArray();
    	array.put(123);
    	array.put(888);

    	List<Integer> result = JsonVisitor.arrayToIntList(array);
    	Assert.assertNotNull(result);
    	Assert.assertEquals(2, result.size());
    	Assert.assertEquals(Integer.valueOf(123), result.get(0));
    	Assert.assertEquals(Integer.valueOf(888), result.get(1));
    }
    
    @Test
    public void arrayToIntList_null() {
    	List<Integer> result = JsonVisitor.arrayToIntList(null);
    	Assert.assertNotNull(result);
    	Assert.assertEquals(0, result.size());
    }
    
    @Test
    public void listToArray() {
    	JSONArray result = JsonVisitor.listToArray(new String[] {"a", "b"});
    	Assert.assertNotNull(result);
    	Assert.assertEquals(2, result.length());
    	Assert.assertEquals("a", result.get(0));
    	Assert.assertEquals("b", result.get(1));
    }
    
    @Test
    public void listToArray_arraylist() {
    	nvJsonObject item = new nvJsonObject();
    	item.set("id", "test");
		JSONArray result = JsonVisitor.listToArray(Arrays.asList(item));
    	Assert.assertNotNull(result);
    	Assert.assertEquals(1, result.length());
    	Assert.assertEquals("{\"id\":\"test\"}", result.get(0).toString());
    }
    
    @Test
    public void collectionToArray() {
    	JSONArray result = JsonVisitor.collectionToArray(Arrays.asList("e", "f"));
    	Assert.assertNotNull(result);
    	Assert.assertEquals(2, result.length());
    	Assert.assertEquals("e", result.get(0));
    	Assert.assertEquals("f", result.get(1));
    }
    
    @Test
    public void collectionToArray_null() {
    	JSONArray result = JsonVisitor.collectionToArray(null);
    	Assert.assertNull(result);
    }
    
    @Test
    public void mapOfStringsToObject() {
    	Map<String, String> map = new HashMap<>();
    	map.put("id", "44");
    	map.put("name", "test");
    	JSONObject result = JsonVisitor.mapOfStringsToObject(map);
    	Assert.assertNotNull(result);
    	Assert.assertEquals(2, result.length());
    	Assert.assertEquals("44", result.get("id"));
    	Assert.assertEquals("test", result.get("name"));
    }
    
    @Test
    public void mapToObject() {
    	Map<String, Integer> map = new HashMap<>();
    	map.put("id", -66);
    	map.put("name", 8000);
    	JSONObject result = JsonVisitor.mapToObject(map);
    	Assert.assertNotNull(result);
    	Assert.assertEquals(2, result.length());
    	Assert.assertEquals(-66, result.get("id"));
    	Assert.assertEquals(8000, result.get("name"));
    }
    
    @Test
    public void forEachElement2_not_null() throws Exception {
    	JSONObject object = generateJsonObject("900");
    	List<String> fieldsList = new ArrayList<>();
    	ObjectVisitor<String, Exception> arrayVisitor = new ObjectVisitor<String, Exception>() {

			@Override
			public boolean visit(String key, String t) throws JSONException, Exception {
				fieldsList.add(key);
				return true;
			}

    	};
    	Assert.assertEquals(0, fieldsList.size());
    	JsonVisitor.forEachElement(object, arrayVisitor);
    	Assert.assertEquals(2, fieldsList.size());
    }
    
    @Test
    public void forEachElement2_null() throws Exception {
    	List<String> fieldsList = new ArrayList<>();
    	ObjectVisitor<String, Exception> arrayVisitor = new ObjectVisitor<String, Exception>() {

			@Override
			public boolean visit(String key, String t) throws JSONException, Exception {
				fieldsList.add(key);
				return true;
			}

    	};
    	Assert.assertEquals(0, fieldsList.size());
    	JsonVisitor.forEachElement(null, arrayVisitor);
    	Assert.assertEquals(0, fieldsList.size());
    }
    
    @Test
    public void forEachElement2_allows_null() throws Exception {
    	JSONObject object = generateJsonObject("null");
    	object.put("nnn", JSONObject.NULL);
    	List<String> fieldsList = new ArrayList<>();
    	ObjectVisitor<String, Exception> arrayVisitor = new ObjectVisitor<String, Exception>() {

			@Override
			public boolean visit(String key, String t) throws JSONException, Exception {
				if(t == null) {
					return false;
				}else {
					fieldsList.add(key);
					return true;
				}
			}

    	};
    	Assert.assertEquals(0, fieldsList.size());
    	JsonVisitor.forEachElement(object, arrayVisitor, true);
    	Assert.assertEquals(2, fieldsList.size());
    }
    
    @Test
    public void forEachElement_allows_null() throws Exception {
    	JSONObject object = generateJsonObject("null");
    	object.put("nnn", "sss");
    	List<String> fieldsList = new ArrayList<>();
    	ObjectVisitor<String, Exception> arrayVisitor = new ObjectVisitor<String, Exception>() {

			@Override
			public boolean visit(String key, String t) throws JSONException, Exception {
				if(t == null) {
					return false;
				}else {
					fieldsList.add(key);
					return false;
				}
			}

    	};
    	Assert.assertEquals(0, fieldsList.size());
    	JsonVisitor.forEachElement(object, arrayVisitor, false);
    	Assert.assertEquals(1, fieldsList.size());
    }
    
    
}
