package io.continual.builder.sources;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.json.JSONObject;
import org.junit.Test;

import junit.framework.TestCase;

public class BuilderJsonDataSourceTest extends TestCase
{
	@Test
	public void testConstructJson ()
	{
		assertNotNull ( new BuilderJsonDataSource ( new JSONObject () ) );
	}

	@Test
	public void testConstructStream ()
	{
		assertNotNull ( new BuilderJsonDataSource ( new ByteArrayInputStream ( "{}".getBytes () ) ) );
	}

	@Test
	public void testConstructReader ()
	{
		try {
			assertNotNull ( new BuilderJsonDataSource ( new FileReader ( "src/test/resources/services.json" ) ) );
		} catch (FileNotFoundException e) {
			fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}

	@Test
	public void testConstructString ()
	{
		assertNotNull ( new BuilderJsonDataSource ( "{}" ) );
	}

	@Test
	public void testGetClassNameFromData ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "classname" , "BuilderJsonDataSource" );
		assertEquals ( "BuilderJsonDataSource" , new BuilderJsonDataSource ( jsonObj ).getClassNameFromData () );

		jsonObj.put ( "class" , "BuilderJsonDataSource" );
		assertEquals ( "BuilderJsonDataSource" , new BuilderJsonDataSource ( jsonObj ).getClassNameFromData () );
	}
}
