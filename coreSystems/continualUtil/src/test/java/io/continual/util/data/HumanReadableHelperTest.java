/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.util.data;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.continual.util.time.Clock;
import io.continual.util.time.Clock.TestClock;
import junit.framework.TestCase;

public class HumanReadableHelperTest extends TestCase
{
	private static final String kCurrencyTests [][]=
	{
		new String[] { "123.45", "123.45"  },
		new String[] { "-123.45", "-123.45"  },
		new String[] { "-1234.56", "-1,234.56"  },
		new String[] { "123.04", "123.04"  },
		new String[] { "6.999", "7.00"  },
	};

	public void testDollarsAndCents ()
	{
		for ( String[] test : kCurrencyTests )
		{
			final double in = Double.parseDouble ( test[0] );
			final String out = HumanReadableHelper.dollarValue ( in );
			assertEquals ( test[1], out );
		}
	}

	private static final String kTimeValueTests [][]=
	{
		new String[] { "12345", "1000", "12 s"  },
		new String[] { "12345", "60000", ""  },
	};

	public void testTimeValues ()
	{
		for ( String[] test : kTimeValueTests )
		{
			final long in = Long.parseLong ( test[0] );
			final long smallest = Long.parseLong ( test[1] );
			final String out = HumanReadableHelper.timeValue ( in, TimeUnit.MILLISECONDS, smallest );
			assertEquals ( test[2], out );
		}
	}

	private static final String kElapsedTimeTests [][]=
	{
		new String[] { "1399999998000", "1000", "-1", "2 s ago"  },
		new String[] { "1399999998000", "60000", "-1", "just now"  },
		new String[] { "3", "0", "-1", "44 yrs, 4 months, 3 wks, 2 days, 16 hrs, 53 m, 19 s, 997 ms ago"  },
		new String[] { "3", "0", "2", "44 yrs, 4 months ago"  },
		new String[] { "1399911440000", "0", "2", "1 day ago"  },
	};

	public void testElapsedTimeValues ()
	{
		final TestClock tc = Clock.useNewTestClock ();
		tc.set ( 1400000000000L );

		for ( String[] test : kElapsedTimeTests )
		{
			final long in = Long.parseLong ( test[0] );
			final long smallest = Long.parseLong ( test[1] );
			final int levels = Integer.parseInt ( test[2] );
			final String out = HumanReadableHelper.elapsedTimeSince ( in, smallest, levels );
			assertEquals ( test[3], out );
		}
	}
	
	public void testParseTypicalDates_wrongFormat() {
		try {
			HumanReadableHelper.parseTypicalDates("asdf");
			fail();
		} catch (ParseException e) {
			// expected to throw exception
		}
	}
	
	public void testParseTypicalDates() throws ParseException {
		assertTrue(new Date().equals(HumanReadableHelper.parseTypicalDates("today")));
	}
	
	public void testParseTypicalDates2() throws ParseException {
		HumanReadableHelper.parseTypicalDates("now");
		assertTrue(new Date().equals(HumanReadableHelper.parseTypicalDates("now")));
	}

	public void testParseTypicalDates3() throws ParseException {
		assertEquals(9000000000000L, HumanReadableHelper.parseTypicalDates("9000000000000").getTime());
	}
	
	public void testParseTypicalDates4() throws ParseException {
		assertEquals(10000L, HumanReadableHelper.parseTypicalDates("10").getTime());
	}
	
	public void testParseTypicalDates_format () throws ParseException
	{
		// this returns a date that's midnight local time, so we can't predict a specific
		// epoch time without specifying a timezone. In this case, we don't provide a TZ
		// so UTC is assumed.
		final Date d = HumanReadableHelper.parseTypicalDates ( "2022-12-31" );
		assertEquals ( 1672444800000L, d.getTime () );
	}
	
	public void testParseDuration_wrongFormat() {
		try {
			HumanReadableHelper.parseDuration("asdf");
			fail();
		} catch (NumberFormatException e) {
			// expected to throw exception
		}
	}
	
	public void testParseDuration() {
		assertEquals(86400000, HumanReadableHelper.parseDuration("1d"));
	}
	
	public void testParseDuration2() {
		assertEquals(86400000, HumanReadableHelper.parseDuration("1days"));
	}
	
	// TODO: Open below tests after bug fix! they will pass
	public void testParseDuration3() {
		assertEquals(3600000, HumanReadableHelper.parseDuration("1h"));
	}
	
	public void testParseDuration4() {
		assertEquals(3600000, HumanReadableHelper.parseDuration("1hr"));
	}
	
	public void testParseDuration5() {
		assertEquals(3600000, HumanReadableHelper.parseDuration("1hrs"));
	}
	
	public void testParseDuration6() {
		assertEquals(60000, HumanReadableHelper.parseDuration("1m"));
	}
	
	public void testParseDuration7() {
		assertEquals(60000, HumanReadableHelper.parseDuration("1min"));
	}
	
	public void testParseDuration8() {
		assertEquals(60000, HumanReadableHelper.parseDuration("1mins"));
	}
	
	public void testElapsedTimeBetween() {
		assertEquals("2 ms ago", HumanReadableHelper.elapsedTimeBetween(4, 2, 0, 1));
	}
	
	public void testElapsedTimeBetween2() {
		assertEquals("2 ms in the future", HumanReadableHelper.elapsedTimeBetween(2, 4, 0, 1));
	}
	
	public void testElapsedTimeBetween3() {
		assertEquals("just now", HumanReadableHelper.elapsedTimeBetween(5, 5, -1, 1));
	}
	
	public void testElapsedTimeBetween_start_end() {
		assertEquals("1 ms in the future", HumanReadableHelper.elapsedTimeBetween(6, 7));
	}

	public void testElapsedTimeSince ()
	{
		final TestClock tc = TestClock.useNewTestClock ();
		tc.set ( 1670204158000L );	// Mon, 05 Dec 2022 01:35:58 GMT

		final LocalDate then = LocalDate.of ( 2001, 9, 9 );
		final int yearDiff = LocalDate.now ().getYear () - then.getYear ();
		final Date thenDate = Date.from ( then.atStartOfDay ( ZoneId.systemDefault () ).toInstant () );
		assertEquals ( yearDiff + " yrs ago", HumanReadableHelper.elapsedTimeSince ( thenDate, 1, 1 ) );
	}
	
	public void testElapsedTimeSince2 ()
	{
		final TestClock tc = TestClock.useNewTestClock ();
		tc.set ( 1670204158000L );	// Mon, 05 Dec 2022 01:35:58 GMT

		final LocalDate then = LocalDate.of ( 2001, 9, 9 );
		final Date thenDate = Date.from ( then.atStartOfDay ( ZoneId.systemDefault () ).toInstant () );
		
		final String elapsed = HumanReadableHelper.elapsedTimeSince ( thenDate.getTime (), 6 );
		assertTrue ( elapsed.startsWith ( "21 yrs, 3 months, 1 day, 21 hrs" ) );
	}

	public void testElapsedTimeSince3 ()
	{
		assertEquals ( "", HumanReadableHelper.elapsedTimeSince ( null, 1 ) );
	}

	public void testElapsedTimeSince4 ()
	{
		final TestClock tc = TestClock.useNewTestClock ();
		tc.set ( 1670204158000L );	// Mon, 05 Dec 2022 01:35:58 GMT

		final String diff = HumanReadableHelper.elapsedTimeSince ( 1000000000000L, 2 );
		assertEquals ( "21 yrs, 3 months, 1 day, 23 hrs, 49 m, 18 s ago", diff );
	}

	public void testElapsedTimeSince6 ()
	{
		assertEquals ( "", HumanReadableHelper.elapsedTimeSince ( null ) );
	}

	public void testDateValue()
	{
		final Date d = new Date ( 1000000000000L );
		final String dstr = HumanReadableHelper.dateValue ( d );
		assertEquals("2001.09.09 01:46:40 UTC", dstr );
	}
	
	public void testPctValue() {
		assertEquals("100032%", HumanReadableHelper.pctValue(1000.32d));
	}
	
	public void testRatioValue() {
		assertEquals("100.32", HumanReadableHelper.ratioValue(100.32d));
	}
	
	public void testRoundedDollarValue() {
		assertEquals("100", HumanReadableHelper.roundedDollarValue(100.32d));
	}
	
	public void testHexDumpLine() {
		List<String> result = HumanReadableHelper.hexDumpLine(new byte[] { 88, 127 }, 0 , 2);
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("58 7F                                               X.", result.get(0));
	}
	
	public void testHexDumpLine_linelenght_short() {
		List<String> result = HumanReadableHelper.hexDumpLine(new byte[] { 1 }, 5 , 2, 4);
		assertNotNull(result);
		assertEquals(0, result.size());
	}
	
	public void testHexDumpLine_linelenght() {
		List<String> result = HumanReadableHelper.hexDumpLine(new byte[] { 10, 22 }, 0 , 2, 4);
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("0A 16           ..", result.get(0));
	}
	
	public void testHexDumpLine_linelenght2() {
		List<String> result = HumanReadableHelper.hexDumpLine(new byte[] { 10, 22 }, 0 , 2, 1);
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals("0A     .", result.get(0));
		assertEquals("16     .", result.get(1));
	}
	
	@Deprecated
	public void testMemoryValue() {
		assertEquals("100 bytes", HumanReadableHelper.memoryValue(100L));
	}
	
	public void testByteCountValue() {
		assertEquals("1.0 EB", HumanReadableHelper.byteCountValue( (long)Math.pow(1000,6) +1 ));
	}

	public void testByteCountValue2() {
		assertEquals("1.0 PB", HumanReadableHelper.byteCountValue( (long)Math.pow(1000,5) +1 ));
	}
	
	public void testByteCountValue3() {
		assertEquals("1.0 TB", HumanReadableHelper.byteCountValue( (long)Math.pow(1000,4) +1 ));
	}
	
	public void testByteCountValue4() {
		assertEquals("1.0 GB", HumanReadableHelper.byteCountValue( (long)Math.pow(1000,3) +1 ));
	}
	
	public void testByteCountValue5() {
		assertEquals("1.0 MB", HumanReadableHelper.byteCountValue( 1000*1000 +1 ));
	}
	
	public void testByteCountValue6() {
		assertEquals("1.0 kB", HumanReadableHelper.byteCountValue( 1000 +1 ));
	}
	
	public void testListOfItems_empty() {
		assertEquals("", HumanReadableHelper.listOfItems(new ArrayList<>(), "", ""));
	}
	
	public void testListOfItems_size1() {
		assertEquals("test", HumanReadableHelper.listOfItems(Arrays.asList("test"), "", ""));
	}
	
	public void testListOfItems_size2() {
		assertEquals("test - me", HumanReadableHelper.listOfItems(Arrays.asList("test","me"), "", "-"));
	}
	
	public void testListOfItems() {
		assertEquals("test*book*- fin", HumanReadableHelper.listOfItems(Arrays.asList("test","book","fin"), "*", "-"));
	}
}
