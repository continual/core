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

package io.continual.restHttp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignedContentReader
{
	public static class BadAuthFormatException extends Exception
	{
		public BadAuthFormatException ( String msg ) { super (msg); }
		private static final long serialVersionUID = 1L;
	}
	
	public interface ApiRequestData
	{
		String getFirstValue ( String string );
		String[] getValuesArray ( String fHeaderAuth );
	}

	public static String getSignedContent ( ApiRequestData req, String dateHeader, String magicHeader, String apiProductTag ) throws BadAuthFormatException
	{
		final String httpDateString = req.getFirstValue ( "Date" );
		final String customDateString = req.getFirstValue ( dateHeader );
		final String apiMagic = req.getFirstValue ( magicHeader );
		return getSignedContent ( httpDateString, customDateString, apiMagic, apiProductTag );
	}

	public static String getSignedContent ( String httpDateString, String customDateString, String apiProductTag ) throws BadAuthFormatException
	{
		return getSignedContent ( httpDateString, customDateString, null, apiProductTag );
	}

	public static String getSignedContent ( String httpDateString, String customDateString, String apiMagic, String apiProductTag ) throws BadAuthFormatException
	{
		// read some headers

		// if a custom date string is provided, use that
		final String dateString = customDateString == null ? httpDateString : customDateString;
		if ( dateString == null )
		{
			authLog ( "Missing date string in header." );
			throw new BadAuthFormatException ( "Couldn't authenticate this request." );
		}

		// parse the date
	    Date result = null;
	    for ( String dateFormat : kDateFormats )
	    {
	        final SimpleDateFormat parser = new java.text.SimpleDateFormat ( dateFormat, java.util.Locale.US );
	        if ( !dateFormat.contains ( "z" ))
	        {
	        	parser.setTimeZone(TIMEZONE_GMT);
	        }

			try
			{
				result = parser.parse ( dateString );
				break;
			}
			catch ( ParseException e )
			{
				// presumably wrong format
			}
	    }
	    if ( result == null )
	    {
			authLog ( "No parser could handle [" + dateString + "]." );
			throw new BadAuthFormatException ( "Couldn't authenticate this request." );
	    }

	    final Date now = new Date ();
	    final long nowMs = now.getTime ();
	    final long thenMs = result.getTime ();
	    final long diffMs = Math.abs ( nowMs - thenMs );
	    if ( diffMs > kMaxTimeDiffMs )
	    {
			authLog ( "[" + dateString + "] is older than " + kMaxTimeDiffMs + " ms, at " + diffMs + " ms from now." );
			throw new BadAuthFormatException ( "Couldn't authenticate this request." );
	    }
	
	    // signed content format is:  apiProductTag + "." + dateString [ + apiMagic ]
	    final StringBuffer sb = new StringBuffer ();
	    sb.append ( apiProductTag );
	    sb.append ( "." );
	    sb.append ( dateString );
	    if ( apiMagic != null )
	    {
	    	sb.append ( apiMagic );
	    }
	    return sb.toString ();
	}

	private static final long kMaxTimeDiffMs = 1000 * 60 * 10;	// 10 minutes
	private static final java.util.TimeZone TIMEZONE_GMT = java.util.TimeZone.getTimeZone("GMT");

	public static final String kPreferredDateFormat = "EEE, dd MMM yyyy HH:mm:ss zzz";
	private static final String kDateFormats[] =
	{
		/* Obsoleted HTTP date format (ANSI C asctime() format). */
	    "EEE MMM dd HH:mm:ss yyyy",

	    /* Obsoleted HTTP date format (RFC 1036). */
	    "EEEE, dd-MMM-yy HH:mm:ss zzz",

	    /* Preferred HTTP date format (RFC 1123). */
	    kPreferredDateFormat,

	    /* W3C date format (RFC 3339). */
	    "yyyy-MM-dd'T'HH:mm:ssz",

	    /* Common date format (RFC 822). */
	    "EEE, dd MMM yy HH:mm:ss z",
	    "EEE, dd MMM yy HH:mm z",
	    "dd MMM yy HH:mm:ss z",
	    "dd MMM yy HH:mm z",

	    /* simple unix command line 'date' format */
	    "EEE MMM dd HH:mm:ss z yyyy"
	};

	private static final Logger log = LoggerFactory.getLogger ( SignedContentReader.class );

	private static final boolean skAuthLogging = true;
	private static void authLog ( String msg )
	{
		if ( skAuthLogging )
		{
			log.info ( msg );
		}
		else
		{
			log.debug ( msg  );
		}
	}
}
