package io.continual.jsonHttpClient.impl.ok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;

public class OkRequestTest
{
	private OkRequest request;
	private OkHttpClient httpClient;

	@Before
	public void setUp()
	{
		httpClient = new OkHttpClient();
		request = new OkRequest(httpClient);
	}

	@Test
	public void testPathWithEncodedSlashPreservation() throws Exception
	{
		final String pathWithEncodedSlash = "https://example.com/api/resource%2Fwith%2Fencoded%2Fslashes";

		request.onPath(pathWithEncodedSlash);

		String actualPath = getPrivateField(request, "fPath");
		assertEquals("Path should preserve encoded slashes", pathWithEncodedSlash, actualPath);
	}

	@Test
	public void testPathWithEncodedSlashAndQueryParams() throws Exception
	{
		final String urlWithQuery = "https://example.com/api/resource%2Fwith%2Fencoded%2Fslashes?param1=value1&param2=value2";
		
		request.onPath(urlWithQuery);

		String actualPath = getPrivateField(request, "fPath");
		HashMap<String,String> queryParams = getPrivateField(request, "fQueryParams");

		assertEquals("Path should preserve encoded slashes without query params", "https://example.com/api/resource%2Fwith%2Fencoded%2Fslashes", actualPath);
		assertNotNull("Query params should be extracted", queryParams);
		assertEquals("Should have correct param1", "value1", queryParams.get("param1"));
		assertEquals("Should have correct param2", "value2", queryParams.get("param2"));
	}

	@Test
	public void testPathWithMultipleEncodedCharacters() throws Exception
	{
		final String complexPath = "https://example.com/api/path%2Fwith%20spaces%2Band%2Fencoded%3Dchars";
		
		request.onPath(complexPath);
		
		String actualPath = getPrivateField(request, "fPath");
		assertEquals("Path should preserve all encoded characters", complexPath, actualPath);
	}

	@Test
	public void testPathWithEncodedSlashInQueryValue() throws Exception
	{
		final String urlWithEncodedSlashInQuery = "https://example.com/api/resource?path=some%2Fencoded%2Fpath&other=value";

		request.onPath(urlWithEncodedSlashInQuery);

		String actualPath = getPrivateField(request, "fPath");
		HashMap<String,String> queryParams = getPrivateField(request, "fQueryParams");

		assertEquals("Path should be clean without query params", "https://example.com/api/resource", actualPath);
		assertNotNull("Query params should be extracted", queryParams);
		assertEquals("Query param don't preserve encoded slash (theyre rebuilt later)", "some/encoded/path", queryParams.get("path"));
		assertEquals("Other param should be correct", "value", queryParams.get("other"));
	}

	@Test
	public void testSimplePathWithoutEncoding() throws Exception
	{
		final String simplePath = "https://example.com/api/simple/path";
		
		request.onPath(simplePath);
		
		String actualPath = getPrivateField(request, "fPath");
		assertEquals("Simple path should remain unchanged", simplePath, actualPath);
	}

	@SuppressWarnings("unchecked")
	private <T> T getPrivateField(Object obj, String fieldName) throws Exception
	{
		Field field = obj.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(obj);
	}
}