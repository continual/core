package io.continual.services.model.core.filters;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.data.ModelObject;
import junit.framework.TestCase;

public class FieldValueEqualsTest extends TestCase
{
	@Test
	public void testBasicFilter ()
	{
		final FieldValueEquals fve = new FieldValueEquals ( "foo", 123 );

		ModelObject mdoa = new JsonModelObject ( new JSONObject () );
		assertFalse ( fve.matches ( mdoa ) );

		mdoa = new JsonModelObject ( new JSONObject ().put ( "foo", "bar" ) );
		assertFalse ( fve.matches ( mdoa ) );

		mdoa = new JsonModelObject ( new JSONObject ().put ( "foo", 123 ) );
		assertTrue ( fve.matches ( mdoa ) );
	}

	@Test
	public void testCompoundFilter ()
	{
		final FieldValueEquals fve = new FieldValueEquals ( "foo.bar", 123 );

		ModelObject mdoa = new JsonModelObject ( new JSONObject () );
		assertFalse ( fve.matches ( mdoa ) );

		mdoa = new JsonModelObject ( new JSONObject ().put ( "foo", "bar" ) );
		assertFalse ( fve.matches ( mdoa ) );

		mdoa = new JsonModelObject ( new JSONObject ().put ( "foo", new JSONObject ().put ( "bar", 123 ) ) );
		assertTrue ( fve.matches ( mdoa ) );
	}

	@Test
	public void testCompoundWithArrayFilter ()
	{
		final FieldValueEquals fve = new FieldValueEquals ( "foo.bar[2]", 123 );

		ModelObject mdoa = new JsonModelObject ( new JSONObject () );
		assertFalse ( fve.matches ( mdoa ) );

		mdoa = new JsonModelObject ( new JSONObject ().put ( "foo", "bar" ) );
		assertFalse ( fve.matches ( mdoa ) );

		mdoa = new JsonModelObject ( new JSONObject ().put ( "foo", new JSONObject ().put ( "bar", 123 ) ) );
		assertFalse ( fve.matches ( mdoa ) );

		mdoa = new JsonModelObject ( new JSONObject ().put ( "foo", new JSONObject ().put ( "bar", new JSONArray ().put ( 0 ).put ( 1 ).put ( 123 ) ) ) );
		assertTrue ( fve.matches ( mdoa ) );
	}
}
