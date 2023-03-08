package io.continual.services.processor.engine.library.util;

import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.engine.model.Message;
import io.continual.util.data.exprEval.ExprDataSource;
import io.continual.util.data.exprEval.JsonDataSource;
import junit.framework.TestCase;

public class SetterTest extends TestCase
{
	@Test
	public void testSettingWithMissingData () throws BuildFailure
	{
		final Message msg = new Message ();

		final ExprDataSource baseData = new JsonDataSource ( new JSONObject ()
			.put ( "foo", "bar" )
			.put ( "obj", new JSONObject ()
				.put ( "val1", "test" )
			)
		);

		final SimpleStreamProcessingContext spc = SimpleStreamProcessingContext.builder ()
			.build ()
		;
		final SimpleMessageProcessingContext mpc = SimpleMessageProcessingContext.builder ()
			.usingContext ( spc )
			.evaluatingAgainst ( baseData )
			.build ( msg );

		final JSONObject obj = new JSONObject ()
			.put ( "a", 123 )
			.put ( "b", "${foo}" )
			.put ( "c", "${obj.val1}" )
			.put ( "d", "${obj.val2}" )
			.put ( "e", "${nonobj.val1}" )
			.put ( "f", new JSONObject ()
				.put ( "f.a", "${obj.val1}" )
				.put ( "f.b", "${nonobj.val1}" )
			)
		;

		final JSONObject result = Setter.evaluate ( mpc, obj, msg );
		assertEquals ( 123, result.getLong ( "a" ) );
		assertEquals ( "bar", result.getString ( "b" ) );
	}
}
