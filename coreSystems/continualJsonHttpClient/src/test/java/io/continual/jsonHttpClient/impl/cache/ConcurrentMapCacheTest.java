package io.continual.jsonHttpClient.impl.cache;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.continual.jsonHttpClient.TestResponse;
import io.continual.util.time.Clock;
import io.continual.util.time.Clock.TestClock;
import junit.framework.TestCase;

public class ConcurrentMapCacheTest extends TestCase
{
	@Test
	public void testBasicCacheUse ()
	{
		final TestClock clock = Clock.useNewTestClock ();

		try ( final ConcurrentMapCache cmc = new ConcurrentMapCache.Builder ()
			.entriesTimingOutAfter ( 30, TimeUnit.SECONDS )
			.build ()
		)
		{
			cmc.put ( "foo", new TestResponse ( 200, "ok" ) );
			assertNotNull ( cmc.get ( "foo" ) );
	
			clock.add ( 31 * 1000L );
			assertNull ( cmc.get ( "foo" ) );
		}
	}
}
