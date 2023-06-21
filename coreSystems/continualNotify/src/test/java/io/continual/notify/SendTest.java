package io.continual.notify;

import org.junit.Ignore;
import org.junit.Test;

import junit.framework.TestCase;

@Ignore	// relies on external service
public class SendTest extends TestCase
{
	@Test
	public void testSimpleSend ()
	{
		ContinualNotifier.send ( "whats up" );
	}

	@Test
	public void testSendOnStream ()
	{
		new ContinualNotifier ()
			.toTopic ( "TEST" )
			.onStream ( "foobar" )
			.withMessage ( "whats up" )
			.send ()
		;
	}
}
