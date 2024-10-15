package io.continual.services.processor.engine.library.processors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import io.continual.services.processor.engine.library.TestProcessingContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import junit.framework.TestCase;

public class ClearTest extends TestCase
{
	@Test
	public void testBasicClear ()
	{
		final MessageProcessingContext mpc = new TestProcessingContext ( new JSONObject ()
			.put ( "foo", "bar" )
			.put ( "baz", 123 )
		);
		final Clear c = new Clear ( new JSONObject ().put ( "keys", new JSONArray().put ( "foo" ) ) );
		c.process ( mpc );

		final java.util.Set<String> keys = mpc.getMessage ().accessRawJson ().keySet ();
		assertFalse ( keys.contains ( "foo" ) );
		assertTrue ( keys.contains ( "baz" ) );
	}

	@Test
	public void testContainedClear ()
	{
		final MessageProcessingContext mpc = new TestProcessingContext ( new JSONObject ()
			.put ( "foo", new JSONObject ()
				.put ( "bar", new JSONObject ()
					.put ( "bee", 123 )
					.put ( "cee", 456 )
				)
			)
			.put ( "baz", 123 )
		);
		final Clear c = new Clear ( new JSONObject ().put ( "keys", new JSONArray().put ( "foo.bar.bee" ) ) );
		c.process ( mpc );

		final JSONObject result = mpc.getMessage ().accessRawJson ();
		final java.util.Set<String> keys = result.keySet ();
		assertTrue ( keys.contains ( "foo" ) );
		assertTrue ( keys.contains ( "baz" ) );
		assertNotNull ( result.optJSONObject ( "foo" ));
		assertNotNull ( result.optJSONObject ( "foo" ).optJSONObject ( "bar" ) );
		assertTrue ( result.optJSONObject ( "foo" ).optJSONObject ( "bar" ).optInt ( "cee", -1 ) == 456 );
		assertTrue ( result.optJSONObject ( "foo" ).optJSONObject ( "bar" ).optInt ( "bee", -1 ) == -1 );
	}

	@Test
	public void testClearNoKey ()
	{
		final MessageProcessingContext mpc = new TestProcessingContext ( new JSONObject ()
			.put ( "foo", "bar" )
			.put ( "baz", 123 )
		);
		final Clear c = new Clear ( new JSONObject ().put ( "keys", new JSONArray().put ( "" ) ) );
		c.process ( mpc );

		final java.util.Set<String> keys = mpc.getMessage ().accessRawJson ().keySet ();
		assertTrue ( keys.contains ( "foo" ) );
		assertTrue ( keys.contains ( "baz" ) );
	}
}
