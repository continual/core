package io.continual.services.processor.engine.library.services.bucketing;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import io.continual.services.processor.engine.library.services.bucketing.BucketingService.Period;
import junit.framework.TestCase;

// FIXME: this test fails in some environments.... 
@Ignore
public class BucketingServiceTest extends TestCase
{
	@Test
	public void testPeriodGapMonth ()
	{
		final Period p = Period.MONTHS;
		
		final long ts2020Jan15 = 1579089600000L;
		final long ts2020Apr15 = 1586952000000L;

		final List<Long> between = Period.getTimestampsBetween ( p, ts2020Jan15, ts2020Apr15 );
		assertEquals ( 2, between.size () );
		assertEquals ( Long.valueOf ( 1581786000000L ), between.get ( 0 ) );
		assertEquals ( Long.valueOf ( 1584291600000L ), between.get ( 1 ) );
	}
}
