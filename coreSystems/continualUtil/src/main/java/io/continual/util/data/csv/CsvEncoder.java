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

package io.continual.util.data.csv;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class CsvEncoder
{
	private CsvEncoder() {
	}
	
	public static final char kDefaultQuoteChar = '"';
	public static final char kDefaultFieldSeparatorChar = ',';

	public static String encodeForCsv ( String val )
	{
		return encodeForCsv ( val, false );
	}

	public static String encodeForCsv ( String val, boolean forceQuotes )
	{
		return encodeForCsv ( val, kDefaultQuoteChar, kDefaultFieldSeparatorChar, forceQuotes );
	}

	public static String encodeForCsv ( String val, char quoteChar, char sepChar )
	{
		return encodeForCsv ( val, quoteChar, sepChar, true );
	}

	public static String encodeForCsv ( String val, char quoteChar, char sepChar, boolean forceQuotes )
	{
		String result = ""; // which means no value at all
		if ( val != null )
		{
			if ( val.length () == 0 )
			{
				// result is "\"\"" -- not the empty string
				result = "" + quoteChar + quoteChar;
			}
			else
			{
				// if the value contains the separator char, it must be escaped and wrapped in quotes
				final boolean needWrap = ( forceQuotes ||
					val.indexOf ( quoteChar ) != -1 ||
					val.indexOf ( sepChar ) != -1 ||
					val.contains ( "\r" ) ||
					val.contains ( "\n" )
				);
				result = escapeString ( val, quoteChar, sepChar, needWrap );
			}
		}
		return result;
	}

	/**
	 * Encode the date into an Excel compatible string in the UTC timezone
	 * @param value the date value
	 * @return a string date
	 */
	public static String encodeForCsv ( Date value )
	{
		return encodeForCsv ( value, UtcTz, true );
	}

	/**
	 * Encode the date into a string in the given timezone, optionally excel compatible.
	 * (If it's not excel compatible, it's ISO8601.)  Also note that Excel's compatibility
	 * is based on the user's locale settings.
	 * 
	 * @param value the date value
	 * @param intoTz the target timezone
	 * @param excelCompatible if true, write a date that excel likes
	 * @return a string date
	 */
	public static String encodeForCsv ( Date value, TimeZone intoTz, boolean excelCompatible )
	{
		// with Excel as the primary target, we do our best to generate a format we think it'll accept.
		// ISO8601 doesn't work in Excel.

		final DateFormat df = new SimpleDateFormat ( excelCompatible ? kGoofyExcelCsvDate : kIso8601Date );
		df.setTimeZone ( intoTz );
		return df.format ( value );
	}

	private static String escapeString ( String val, char quoteChar, char sepChar, boolean needWrap )
	{
		final StringBuffer result = new StringBuffer ();
		if (needWrap) result.append ( quoteChar );

		final int len = val.length ();
		for ( int i=0; i<len; i++ )
		{
			final char c = val.charAt ( i );
			if ( c == quoteChar )
			{
				// add an extra one
				result.append ( c );
			}
			result.append ( c );
		}
		if (needWrap) result.append ( quoteChar );

		return result.toString ();
	}

	private static final TimeZone UtcTz = TimeZone.getTimeZone ( "UTC" );
	private static final String kGoofyExcelCsvDate = "MM/dd/yy HH:mm:ss";
	private static final String kIso8601Date = "yyyy-MM-dd'T'HH:mm'Z'";
}
