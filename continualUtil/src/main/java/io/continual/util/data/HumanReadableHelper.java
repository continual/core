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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.continual.util.time.Clock;

public class HumanReadableHelper
{
	private static final long kMultiplier = 1000;
	private static final long kKilobyte = kMultiplier;
	private static final long kMegabyte = kMultiplier * kKilobyte;
	private static final long kGigabyte = kMultiplier * kMegabyte;
	private static final long kTerabyte = kMultiplier * kGigabyte;
	private static final long kPetabyte = kMultiplier * kTerabyte;
	private static final long kExabyte = kMultiplier * kPetabyte;

	public static String byteCountValue ( long inBytes )
	{
		String result = "" + inBytes + " bytes";
		if ( inBytes > kExabyte )
		{
			double d = inBytes / kExabyte;
			result = "" + d + " EB";
		}
		else if ( inBytes > kPetabyte )
		{
			double d = inBytes / kPetabyte;
			result = "" + d + " PB";
		}
		else if ( inBytes > kTerabyte )
		{
			double d = inBytes / kTerabyte;
			result = "" + d + " TB";
		}
		else if ( inBytes > kGigabyte )
		{
			double d = inBytes / kGigabyte;
			result = "" + d + " GB";
		}
		else if ( inBytes > kMegabyte )
		{
			double d = inBytes / kMegabyte;
			result = "" + d + " MB";
		}
		else if ( inBytes > kKilobyte )
		{
			double d = inBytes / kKilobyte;
			result = "" + d + " kB";
		}
		return result;
	}

	@Deprecated
	public static String memoryValue ( long inBytes )
	{
		return byteCountValue ( inBytes );
	}

	public static List<String> hexDumpLine ( byte[] bytes, int offset, int length )
	{
		return hexDumpLine ( bytes, offset, length, 16 );
	}

	public static List<String> hexDumpLine ( byte[] bytes, int offset, int length, int lineLengthInBytes )
	{
		final LinkedList<String> result = new LinkedList<> ();
		
		while ( offset < length )
		{
			// do the next line...
			final int bytesRemaining = length - offset;
			final int bytesThisLine = offset + Math.min ( bytesRemaining, lineLengthInBytes );

			final StringBuilder hexPart = new StringBuilder ();
			final StringBuilder asciiPart = new StringBuilder ();

			while ( offset < bytesThisLine )
			{
				int ch = bytes[offset];
				final String byteInHex = TypeConvertor.byteToHex ( ch );
				final char byteAsChar = isPrintableForHexDump ( ch ) ? (char) ch : '.';

				hexPart
					.append ( byteInHex )
					.append ( ' ' )
				;
				asciiPart
					.append ( byteAsChar )
				;

				offset++;
			}

			final StringBuilder line = new StringBuilder ()
				.append ( hexPart.toString ()  )
			;
			while ( line.length () < ( lineLengthInBytes * 3 + 4 ) )
			{
				line.append ( ' ' );
			}
			line.append ( asciiPart.toString () );
			result.add ( line.toString () );
		}

		return result;
	}
	
	private static boolean isPrintableForHexDump ( int ch )
	{
		return ch >= 32 && ch <= 126;
	}

	public static final long kMillisecond = 1;
	public static final long kSecond = 1000 * kMillisecond;
	public static final long kMinute = 60 * kSecond;
	public static final long kHour = 60 * kMinute;
	public static final long kDay = 24 * kHour;
	public static final long kWeek = 7 * kDay;
	public static final long kMonth = 30 * kDay;
	public static final long kYear = 365 * kDay;

	/**
	 * Extend a number with 0s to a required width. Intended for decimal-side extension.
	 * @param v a long value
	 * @return a string extended to the required length
	 */
	private static String buildCents ( long v )
	{
		if ( v < 10 )
		{
			return "0" + v;
		}
		else
		{
			String s = "" + v;
			while ( s.length () < 2 )
			{
				s = s + "0";
			}
			return s;
		}
	}

	public static String dollarValue ( double d )
	{
		final boolean negate = d < 0.0;
		if ( negate ) d = d*-1.0;
		final long dollars = Math.round ( Math.floor ( d ) );
		final long cents = Math.round ( ( d - dollars ) * 100 );
		return (negate?"-":"") + numberValue ( dollars ) + "." + buildCents ( cents );
	}

	public static String roundedDollarValue ( double d )
	{
		return numberValue ( Math.round ( d ) );
	}

	public static String numberValue ( long units )
	{
		final StringBuffer sb = new StringBuffer ();

		final String raw = "" + units;
		final int count = raw.length ();
		final int firstPart = count % 3;
		int printed = 3 - firstPart;
		for ( int i=0; i<count; i++ )
		{
			if ( printed == 3 )
			{
				if ( sb.length () > 0 )
				{
					sb.append ( ',' );
				}
				printed = 0;
			}
			sb.append ( raw.charAt ( i ) );
			printed++;
		}

		return sb.toString ();
	}

	public static String ratioValue ( double d )
	{
		// FIXME: use formatter
		double rounded2 = Math.round ( d * 100 ) / 100.0;
		return "" + rounded2;
	}

	public static String pctValue ( double d )
	{
		// FIXME: use formatter
		final long pct = Math.round ( d * 100 );
		return "" + pct + "%";
	}

	public static String dateValue ( Date d )
	{
		return sdf.format ( d );
	}
	private static final SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy.MM.dd HH:mm:ss z" );

	public static String elapsedTimeSince ( Date d )
	{
		if ( d == null ) return "";
		return elapsedTimeSince ( d.getTime () );
	}

	public static String elapsedTimeSince ( long epochMs )
	{
		// return elapsed time with precision that's scaled back as the time grows distant
		return elapsedTimeSince ( epochMs, 1, 2 );
	}

	public static String elapsedTimeSince ( Date d, long smallestUnitInMillis )
	{
		if ( d == null ) return "";
		return elapsedTimeSince ( d.getTime (), smallestUnitInMillis );
	}

	public static String elapsedTimeSince ( long epochMs, long smallestUnitInMillis )
	{
		return elapsedTimeSince ( epochMs, smallestUnitInMillis, -1 );
	}

	public static String elapsedTimeSince ( Date epochMs, long smallestUnitInMillis, int maxLevels )
	{
		return elapsedTimeSince ( epochMs.getTime (), smallestUnitInMillis, maxLevels );
	}

	public static String elapsedTimeSince ( long epochMs, long smallestUnitInMillis, int maxLevels )
	{
		final long elapsedMs = Clock.now () - epochMs;
		if ( elapsedMs < 0 )
		{
			final String amt = timeValue ( elapsedMs * -1, TimeUnit.MILLISECONDS, smallestUnitInMillis, maxLevels );
			if ( amt == null || amt.length() == 0 ) return "just now";
			return amt + " in the future";
		}
		else
		{
			final String amt = timeValue ( elapsedMs, TimeUnit.MILLISECONDS, smallestUnitInMillis, maxLevels );
			if ( amt == null || amt.length() == 0 ) return "just now";
			return amt + " ago";
		}
	}

	public static String timeValue ( long units, TimeUnit tu, long smallestUnit )
	{
		return timeValue ( units, tu, smallestUnit, -1 );
	}

	public static String timeValue ( long units, TimeUnit tu, long smallestUnit, int maxLevels )
	{
		return timeValue ( new TimeValueContext ( units, tu, smallestUnit, maxLevels ) );
	}

	/**
	 * parse a time duration like "6h" or "10d" into ms
	 * @param duration a time duration string
	 * @return milliseconds
	 */
	public static long parseDuration ( String duration )
	{
		if ( duration.endsWith ( "d" ) || duration.endsWith ( "days" ) )
		{
			final String valueStr = duration.substring ( 0, duration.indexOf ( "d" ) );
			final double value = Double.parseDouble ( valueStr );
			final double asMs = value * (24.0 * 60.0 * 60.0 * 1000.0);
			return Math.round ( asMs );
		}
		else if ( duration.endsWith ( "h" ) || duration.endsWith ( "hr" ) || duration.endsWith ( "hrs" ) )
		{
			final String valueStr = duration.substring ( 0, duration.indexOf ( "d" ) );
			final double value = Double.parseDouble ( valueStr );
			final double asMs = value * (60.0 * 60.0 * 1000.0);
			return Math.round ( asMs );
		}
		else if ( duration.endsWith ( "m" ) || duration.endsWith ( "min" ) || duration.endsWith ( "mins" ) )
		{
			final String valueStr = duration.substring ( 0, duration.indexOf ( "d" ) );
			final double value = Double.parseDouble ( valueStr );
			final double asMs = value * (60.0 * 1000.0);
			return Math.round ( asMs );
		}
		else
		{
			throw new NumberFormatException ( "Can't parse duration [" + duration + "]" );
		}
	}

	/**
	 * Parse date strings as they're typically seen in configuration or input. Note that this isn't tuned to be
	 * especially fast -- use it for occasional interpretation, not high volume transactions.
	 * @param d a date string 
	 * @return a date
	 * @throws ParseException if the date's format is unrecognizable 
	 */
	public static Date parseTypicalDates ( String d ) throws ParseException
	{
		if ( d.equalsIgnoreCase ( "today" ) || d.equalsIgnoreCase ( "now" ) )
		{
			return new Date ();
		}

		try
		{
			long number = Long.parseLong ( d );
			if ( number > kMsThreshold )
			{
				return new Date ( number );
			}
			else
			{
				return new Date ( number * 1000L );
			}
		}
		catch ( NumberFormatException x )
		{
			// ignore
		}
		
		for ( SimpleDateFormat sdf : kDateFormats )
		{
			try
			{
				return sdf.parse ( d );
			}
			catch ( ParseException x )
			{
				// skip it
			}
		}
		throw new ParseException ( "Unrecognized date [" + d +  "].", 0 );
	}

	private static SimpleDateFormat[] kDateFormats =
	{
		new SimpleDateFormat ( "yyyy-MM-dd" ),
		new SimpleDateFormat ( "MM/dd/yyyy" ),
	};
	private static final long kMsThreshold = ( System.currentTimeMillis () / 100L );

	private static long[] kTimeVals = { kYear, kMonth, kWeek, kDay, kHour, kMinute, kSecond, kMillisecond };
	private static String[] kTimeValAbbvsSingle = { "yr", "month", "wk", "day", "hr", "m", "s", "ms" }; 
	private static String[] kTimeValAbbvsPlural = { "yrs", "months", "wks", "days", "hrs", "m", "s", "ms" }; 

	private static class TimeValueContext
	{
		public TimeValueContext ( long units, TimeUnit tu, long smallestUnit, int maxLevels )
		{
			fTimeValueMs = TimeUnit.MILLISECONDS.convert ( units, tu );
			fRemainingMs = fTimeValueMs;
			fSmallestUnit = smallestUnit;
			fMaxLevels = maxLevels;
		}
		public final long fTimeValueMs;
		public long fRemainingMs;
		public long fSmallestUnit;
		public int fMaxLevels;
	};

	private static String timeValue ( TimeValueContext tvc )
	{
		final StringBuffer result = new StringBuffer ();

		int firstUnitIndex = -1;
		for ( int unit = 0; unit < kTimeVals.length; unit++ )
		{
			if ( kTimeVals[unit] < tvc.fSmallestUnit )
			{
				break;
			}
			if ( tvc.fMaxLevels > -1 && firstUnitIndex > -1 && unit >= firstUnitIndex + tvc.fMaxLevels )
			{
				break;
			}
			
			if ( tvc.fRemainingMs >= kTimeVals[unit] )
			{
				// extract the value for this unit
				final long count = tvc.fRemainingMs / kTimeVals[unit];
				tvc.fRemainingMs = tvc.fRemainingMs - ( count * kTimeVals[unit] );

				// update the text string
				if ( firstUnitIndex >= 0 ) result.append ( ", " );
				result
					.append ( count )
					.append ( " " )
					.append ( count == 1 ? kTimeValAbbvsSingle[unit] : kTimeValAbbvsPlural[unit] )
				;

				if ( firstUnitIndex < 0 ) firstUnitIndex = unit;
			}
		}
		return result.toString ();
	}

	/**
	 * Return a string that is a list of items separated by separator and using
	 * the final conjunction. For example, { "A", "B", "C" } --&gt; "A, B, and C"
	 * @param items the input list
	 * @param separator the separator to use in the sequence
	 * @param finalConjunction the final conjunction (e.g. "and") 
	 * @return a list of items
	 */
	public static String listOfItems ( List<String> items, String separator, String finalConjunction )
	{
		final int size = items.size ();
		if ( size < 1 ) return "";

		switch ( size )
		{
			case 1:
				return items.iterator ().next ();

			case 2:
				return items.get ( 0 ) + " " + finalConjunction + " " + items.get ( 1 );

			default:
			{
				final StringBuffer result = new StringBuffer ();
				for ( int i=0; i<size-1; i++ )
				{
					result.append ( items.get ( i ) );
					result.append ( separator );
				}
				result.append ( finalConjunction );
				result.append ( " " );
				result.append ( items.get ( size - 1 ) );
				return result.toString ();
			}
		}
	}
}
