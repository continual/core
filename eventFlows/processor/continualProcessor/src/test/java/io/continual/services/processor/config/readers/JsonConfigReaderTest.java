package io.continual.services.processor.config.readers;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import io.continual.services.Service.FailedToStart;
import io.continual.services.processor.engine.library.processors.SendToSink;
import io.continual.services.processor.engine.library.processors.Set;
import io.continual.services.processor.engine.library.sinks.JsonObjectStreamSink;
import io.continual.services.processor.engine.library.sources.JsonObjectStreamSource;
import io.continual.services.processor.engine.model.Program;
import io.continual.services.processor.engine.runtime.Engine;
import junit.framework.TestCase;

public class JsonConfigReaderTest extends TestCase
{
	@Test
	public void testEvalBlockFlagsByDefault () throws ConfigReadException, InterruptedException, FailedToStart
	{
		final JSONObject config = getBaseConfig ();
		final Program prog = new JsonConfigReader ().read ( config );

		final JsonObjectStreamSource src = (JsonObjectStreamSource) prog.getSources ().get ( "testIn" );
		final JsonObjectStreamSink sink = (JsonObjectStreamSink) prog.getSinks ().get ( "testOut" );

		assertNotNull ( src );
		assertNotNull ( sink );

		src.submit ( new JSONObject ().put ( "foo", 123 ) );
		src.noteEndOfStream ();

		final Engine e = new Engine ( prog );
		e.startAndWait ();

		final List<JSONObject> output = sink.getList ();
		assertTrue ( output.size () == 1 );

		System.out.println ( output.get ( 0 ).toString () );

		assertEquals ( "", output.get ( 0 ).optString ( "bar", null ) );
	}

	@Test
	public void testDisableEvalBlockFlagsGlobally () throws ConfigReadException, InterruptedException, FailedToStart
	{
		final JSONObject config = getBaseConfig ();
		config.put ( JsonConfigReader.kEvalOnLoad, false );
		final Program prog = new JsonConfigReader ().read ( config );

		final JsonObjectStreamSource src = (JsonObjectStreamSource) prog.getSources ().get ( "testIn" );
		final JsonObjectStreamSink sink = (JsonObjectStreamSink) prog.getSinks ().get ( "testOut" );

		assertNotNull ( src );
		assertNotNull ( sink );

		src.submit ( new JSONObject ().put ( "foo", 123 ) );
		src.noteEndOfStream ();

		final Engine e = new Engine ( prog );
		e.startAndWait ();

		final List<JSONObject> output = sink.getList ();
		assertTrue ( output.size () == 1 );

		System.out.println ( output.get ( 0 ).toString () );

		assertEquals ( "123", output.get ( 0 ).optString ( "bar", null ) );
	}

	@Test
	public void testDisableEvalBlockFlagsGloballyAndEnableLocally () throws ConfigReadException, InterruptedException, FailedToStart
	{
		final JSONObject config = getBaseConfig ();
		config.put ( JsonConfigReader.kEvalOnLoad, false );
		config.getJSONObject ( "pipelines" ).getJSONArray ( "p" ).getJSONObject ( 0 ).getJSONArray ( "always" ).getJSONObject ( 0 ).put ( JsonConfigReader.kEvalOnLoad, true );
		
		final Program prog = new JsonConfigReader ().read ( config );

		final JsonObjectStreamSource src = (JsonObjectStreamSource) prog.getSources ().get ( "testIn" );
		final JsonObjectStreamSink sink = (JsonObjectStreamSink) prog.getSinks ().get ( "testOut" );

		assertNotNull ( src );
		assertNotNull ( sink );

		src.submit ( new JSONObject ().put ( "foo", 123 ) );
		src.noteEndOfStream ();

		final Engine e = new Engine ( prog );
		e.startAndWait ();

		final List<JSONObject> output = sink.getList ();
		assertTrue ( output.size () == 1 );

		System.out.println ( output.get ( 0 ).toString () );

		assertEquals ( "", output.get ( 0 ).optString ( "bar", null ) );
	}

	@Test
	public void testEnableEvalBlockFlagsGloballyAndDisableLocally () throws ConfigReadException, InterruptedException, FailedToStart
	{
		final JSONObject config = getBaseConfig ();
		config.put ( JsonConfigReader.kEvalOnLoad, true );
		config.getJSONObject ( "pipelines" ).getJSONArray ( "p" ).getJSONObject ( 0 ).getJSONArray ( "always" ).getJSONObject ( 0 ).put ( JsonConfigReader.kEvalOnLoad, false );
		
		final Program prog = new JsonConfigReader ().read ( config );

		final JsonObjectStreamSource src = (JsonObjectStreamSource) prog.getSources ().get ( "testIn" );
		final JsonObjectStreamSink sink = (JsonObjectStreamSink) prog.getSinks ().get ( "testOut" );

		assertNotNull ( src );
		assertNotNull ( sink );

		src.submit ( new JSONObject ().put ( "foo", 123 ) );
		src.noteEndOfStream ();

		final Engine e = new Engine ( prog );
		e.startAndWait ();

		final List<JSONObject> output = sink.getList ();
		assertTrue ( output.size () == 1 );

		System.out.println ( output.get ( 0 ).toString () );

		assertEquals ( "123", output.get ( 0 ).optString ( "bar", null ) );
	}

	private static JSONObject getBaseConfig ()
	{
		return new JSONObject ()
			.put ( "sources", new JSONObject ()
				.put ( "testIn", new JSONObject ()
					.put ( "class", JsonObjectStreamSource.class.getName () )
					.put ( "pipeline", "p" )
				)
			)
			.put ( "sinks", new JSONObject ()
				.put ( "testOut", new JSONObject ()
					.put ( "class", JsonObjectStreamSink.class.getName () )
				)
			)
			.put ( "pipelines", new JSONObject ()
				.put ( "p", new JSONArray ()
					.put ( new JSONObject ()
						.put ( "always", new JSONArray ()
							.put ( new JSONObject ()
								.put ( "class", Set.class.getName () )
								.put ( "updates", new JSONObject ()
									.put ( "bar", "${foo}" )
								)
							)
							.put ( new JSONObject ()
								.put ( "class", SendToSink.class.getName () )
								.put ( "to", "testOut" )
							)
						)
					)
				)
			)
		;
	}
}
