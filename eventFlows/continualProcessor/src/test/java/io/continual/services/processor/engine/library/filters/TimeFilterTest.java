package io.continual.services.processor.engine.library.filters;

import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.engine.library.TestProcessingContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.util.time.Clock;
import junit.framework.TestCase;

public class TimeFilterTest extends TestCase
{
	@Test
	public void testEmptyTimeFilter () throws BuildFailure
	{
		final TimeFilter f = new TimeFilter ( null, new JSONObject ()
			.put ( "expr", "${time}" )
		);

		final MessageProcessingContext msg = makeMessage ( new JSONObject ().put ( "time", 1L ));
		assertTrue ( f.passes ( msg  ) );
	}

	@Test
	public void testTimeAtLeastAbsFilter () throws BuildFailure
	{
		final TimeFilter f = new TimeFilter ( null, new JSONObject ()
			.put ( "expr", "${time}" )
			.put ( "lowerLimit", "12345" )
		);

		final MessageProcessingContext msg = makeMessage ( new JSONObject ().put ( "time", 1L ));
		assertFalse ( f.passes ( msg ) );

		final MessageProcessingContext msg2 = makeMessage ( new JSONObject ().put ( "time", 12346L ));
		assertTrue ( f.passes ( msg2 ) );
	}

	@Test
	public void testTimeAtLeastRelFilter () throws BuildFailure
	{
		final long baseTimeMs = 14000000000L;
		Clock.useNewTestClock ()
			.set ( baseTimeMs )
		;

		final TimeFilter f = new TimeFilter ( null, new JSONObject ()
			.put ( "expr", "${time}" )
			.put ( "lowerLimit", "-1h" )
		);

		// 5 hours ago
		final MessageProcessingContext msg1 = makeMessage ( new JSONObject ().put ( "time", baseTimeMs - (1000L*60*60*5) ));
		assertFalse ( f.passes ( msg1 ) );

		// 5 minutes ago
		final MessageProcessingContext msg2 = makeMessage ( new JSONObject ().put ( "time", baseTimeMs - (1000L*60*5) ));
		assertTrue ( f.passes ( msg2 ) );
	}

	private static MessageProcessingContext makeMessage ( JSONObject msgData )
	{
		return new TestProcessingContext ( msgData );
	}
}
