package io.continual.util.data.json;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class JsonObjectMapTest {
	
    public JsonObjectMap generate(){
    	JSONObject jsonObject = new JSONObject();
    	jsonObject.put("name", "Arzu");
    	jsonObject.put("surname", "sari");
		JsonObjectMap jsonObjectMap = new JsonObjectMap(jsonObject);
		return jsonObjectMap;
    }
	
	@Test
    public void size(){
		JsonObjectMap jsonObjectMap = generate();
		Assert.assertEquals(2, jsonObjectMap.size());
    }
	
	@Test
    public void isEmpty(){
		JsonObjectMap jsonObjectMap = generate();
		Assert.assertEquals(false, jsonObjectMap.isEmpty());
    }
	
	@Test
    public void isEmpty_negative(){
		JsonObjectMap jsonObjectMap = new JsonObjectMap(new JSONObject());
		Assert.assertEquals(true, jsonObjectMap.isEmpty());
    }
	
	@Test
    public void containsKey(){
		JsonObjectMap jsonObjectMap = generate();
		Assert.assertEquals(true, jsonObjectMap.containsKey("name"));
    }
	
	@Test
    public void get(){
		JsonObjectMap jsonObjectMap = generate();
		Assert.assertEquals("Arzu", jsonObjectMap.get("name"));
    }
	
	@Test
    public void put(){
		JsonObjectMap jsonObjectMap = generate();
		Assert.assertEquals("", jsonObjectMap.put("age", "40"));
    }
	
	@Test
    public void remove(){
		JsonObjectMap jsonObjectMap = generate();
		Assert.assertEquals("Arzu", jsonObjectMap.remove("name"));
		Assert.assertEquals(1, jsonObjectMap.size());
    }
	
	@Test
    public void putAll(){
		JsonObjectMap jsonObjectMap = generate();
		Map<String, String> map = new HashMap<>();
		map.put("age", "40");
		map.put("username", "User A");
		jsonObjectMap.putAll(map);
		Assert.assertEquals(4, jsonObjectMap.size());
		Assert.assertEquals("40", jsonObjectMap.get("age"));
		Assert.assertEquals("User A", jsonObjectMap.get("username"));
    }
	
	@Test
    public void clear(){
		JsonObjectMap jsonObjectMap = generate();
		jsonObjectMap.clear();
		Assert.assertEquals(0, jsonObjectMap.size());
    }

	@Test
    public void containsValueTest(){
		JsonObjectMap jsonObjectMap = generate();
		Assert.assertEquals(true, jsonObjectMap.containsKey("name"));
    }
	
//	@Test
//	public void valuesTest() {
//
//		JsonObjectMap jsonObjectMap = generate();
//		
//        Collection<String> values = jsonObjectMap.values();
//
//        Collection<String> expectedValues = new TreeSet<>();
//        expectedValues.add("value1");
//        expectedValues.add("value2");
//        expectedValues.add("value3");
//
//        assertEquals(expectedValues, values);
//	}
	

	
}
