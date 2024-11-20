package io.continual.services.processor.engine.library.processors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.engine.library.TestProcessingContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import junit.framework.TestCase;

public class ForEachObjectTest extends TestCase
{
	@Test
	public void testBasicForEach () throws JSONException, BuildFailure
	{
		final MessageProcessingContext mpc = new TestProcessingContext ( new JSONObject ()
			.put ( "foo", "bar" )
			.put ( "baz", 123 )
			.put ( "objects", new JSONArray ()
				.put ( new JSONObject ().put ( "name", "Alice" ) )
				.put ( new JSONObject ().put ( "name", "Bob" ) )
			)
		);
		final ForEachObject feo = new ForEachObject ( null, new JSONObject ()
			.put ( "set", "objects" )
			.put ( "processing", new JSONArray () )
		);
		feo.process ( mpc );
	}
}
