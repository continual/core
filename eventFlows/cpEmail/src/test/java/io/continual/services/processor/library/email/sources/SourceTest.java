package io.continual.services.processor.library.email.sources;

import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.Service.FailedToStart;
import io.continual.services.ServiceContainer;
import io.continual.services.processor.engine.library.processors.SendToSink;
import io.continual.services.processor.engine.library.sinks.FileSink;
import io.continual.services.processor.engine.model.Pipeline;
import io.continual.services.processor.engine.model.Program;
import io.continual.services.processor.engine.model.Rule;
import io.continual.services.processor.engine.runtime.Engine;
import junit.framework.TestCase;

public class SourceTest extends TestCase
{
	@Test
	public void testAccess () throws FailedToStart, BuildFailure
	{
		final ServiceContainer sc = new ServiceContainer ();
		final JSONObject config = new JSONObject ()
			.put ( EmailInboxReader.kSetting_MailLogin, "${MAIL_USER}" )
			.put ( EmailInboxReader.kSetting_MailPassword, "${MAIL_PASSWORD}" )
			.put ( ImapMailboxMonitor.kSetting_PollFreqMinutes, 1 )
			.put ( "tracker", new JSONObject ()
				.put ( "class", FileSeenTracker.class.getName () )
				.put ( "file", "/tmp/emailtrack.txt" )
			)
		;
		
		final Program prog = new Program ()
			.addSource ( "email", new ImapMailboxMonitor ( sc, config ) )
			.addSink ( "out", new FileSink () )
			.addPipeline ( Program.kDefaultPipeline, new Pipeline().addRule ( Rule.newRule().alwaysDo ( new SendToSink ( "out" ) ).build () ) )
		;
		final Engine e = new Engine ( null, prog, 60000 );
		e.startAndWait ();
	}
}
