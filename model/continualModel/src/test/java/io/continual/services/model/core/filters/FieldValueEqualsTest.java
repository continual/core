package io.continual.services.model.core.filters;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import io.continual.services.model.core.data.JsonObjectAccess;
import io.continual.services.model.core.data.ModelDataObjectAccess;
import junit.framework.TestCase;

public class FieldValueEqualsTest extends TestCase
{
	@Test
	public void testBasicFilter ()
	{
		final FieldValueEquals fve = new FieldValueEquals ( "foo", 123 );

		ModelDataObjectAccess mdoa = new JsonObjectAccess ( new JSONObject () );
		assertFalse ( fve.matches ( mdoa ) );

		mdoa = new JsonObjectAccess ( new JSONObject ().put ( "foo", "bar" ) );
		assertFalse ( fve.matches ( mdoa ) );

		mdoa = new JsonObjectAccess ( new JSONObject ().put ( "foo", 123 ) );
		assertTrue ( fve.matches ( mdoa ) );
	}

	@Test
	public void testCompoundFilter ()
	{
		final FieldValueEquals fve = new FieldValueEquals ( "foo.bar", 123 );

		ModelDataObjectAccess mdoa = new JsonObjectAccess ( new JSONObject () );
		assertFalse ( fve.matches ( mdoa ) );

		mdoa = new JsonObjectAccess ( new JSONObject ().put ( "foo", "bar" ) );
		assertFalse ( fve.matches ( mdoa ) );

		mdoa = new JsonObjectAccess ( new JSONObject ().put ( "foo", new JSONObject ().put ( "bar", 123 ) ) );
		assertTrue ( fve.matches ( mdoa ) );
	}

	@Test
	public void testCompoundWithArrayFilter ()
	{
		final FieldValueEquals fve = new FieldValueEquals ( "foo.bar[2]", 123 );

		ModelDataObjectAccess mdoa = new JsonObjectAccess ( new JSONObject () );
		assertFalse ( fve.matches ( mdoa ) );

		mdoa = new JsonObjectAccess ( new JSONObject ().put ( "foo", "bar" ) );
		assertFalse ( fve.matches ( mdoa ) );

		mdoa = new JsonObjectAccess ( new JSONObject ().put ( "foo", new JSONObject ().put ( "bar", 123 ) ) );
		assertFalse ( fve.matches ( mdoa ) );

		mdoa = new JsonObjectAccess ( new JSONObject ().put ( "foo", new JSONObject ().put ( "bar", new JSONArray ().put ( 0 ).put ( 1 ).put ( 123 ) ) ) );
		assertTrue ( fve.matches ( mdoa ) );
	}
}
