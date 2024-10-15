package io.continual.services.processor.library.airtable.sinks;

import java.io.IOException;

import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import com.rathravane.airtableClient.AirtableClient.AirtableRequestException;
import com.rathravane.airtableClient.AirtableClient.AirtableServiceException;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.processor.engine.library.util.SimpleMessageProcessingContext;
import io.continual.services.processor.engine.library.util.SimpleStreamProcessingContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import junit.framework.TestCase;

@Ignore
public class AirtableSinkTest extends TestCase
{
	private static final String kKeyField = "Name";
	private static final String kNotesField = "Notes";
	
	@Test
	public void testSink () throws BuildFailure, AirtableRequestException, AirtableServiceException, IOException
	{
		final ServiceContainer sc = new ServiceContainer ();
		final ExpressionEvaluator ee = sc.getExprEval ();

		try ( final AirtableSink sink = new AirtableSink (
			sc,
			new JSONObject ()
				.put ( "base", ee.evaluateText ( "${BASE_ID}" ) )
				.put ( "apiKey", ee.evaluateText ( "${APIKEY}" ) )
				.put ( "table", ee.evaluateText ( "${TABLE}" ) )
				.put ( "key", kKeyField )
				.put ( "writeStrategy", new JSONObject ()
					.put ( kNotesField, AirtableSink.FieldWriteStrategy.APPEND.toString () )
				)
			) )
		{
			sink.init ();
	
			final SimpleStreamProcessingContext spc = SimpleStreamProcessingContext.builder ()
				.build ()
			;
			final Message msg = new Message ()
				.putValue ( kKeyField, ee.evaluateText ( "${TEST_KEY}" ) )
				.putValue ( kNotesField, ee.evaluateText ( "${TEST_NOTES}" ) )
			;
			final SimpleMessageProcessingContext mpc = new SimpleMessageProcessingContext.Builder ()
				.usingContext ( spc )
				.build ( msg )
			;
	
			// process
			sink.process ( mpc );
		}
	}
}
