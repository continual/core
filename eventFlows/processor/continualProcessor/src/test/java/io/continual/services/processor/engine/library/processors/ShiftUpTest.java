package io.continual.services.processor.engine.library.processors;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.engine.library.TestProcessingContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import junit.framework.TestCase;

public class ShiftUpTest extends TestCase
{
	@Test
	public void testBasicShiftUp () throws JSONException, BuildFailure
	{
		final MessageProcessingContext mpc = new TestProcessingContext ( new JSONObject ()
			.put ( "foo", "bar" )
			.put ( "baz", 123 )
		);
		final ShiftUp c = new ShiftUp ( null, new JSONObject ()
			.put ( "to", "newtop" )
		);
		c.process ( mpc );

		final JSONObject result = mpc.getMessage ().accessRawJson ();
		assertNotNull ( result );
		assertTrue ( result.has ( "newtop" ) );
	}
}
