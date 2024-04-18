package io.continual.services.processor.engine.library.processors;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.engine.library.TestProcessingContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.util.data.json.JsonUtil;
import junit.framework.TestCase;

public class ShiftDownTest extends TestCase
{
	@Test
	public void testBasicShiftDown () throws JSONException, BuildFailure
	{
		final JSONObject inner = new JSONObject ().put ( "bar", false ).put ( "buzz", "bbbb" ); 
		final MessageProcessingContext mpc = new TestProcessingContext ( new JSONObject ()
			.put ( "foo", inner )
			.put ( "baz", 123 )
		);
		final ShiftDown c = new ShiftDown ( null, new JSONObject ()
			.put ( "to", "foo" )
		);
		c.process ( mpc );

		final JSONObject result = mpc.getMessage ().accessRawJson ();
		assertNotNull ( result );
		assertEquals ( JsonUtil.hash ( result ), JsonUtil.hash ( inner ) );
	}
}
