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

import java.util.LinkedList;
import java.util.List;

import io.continual.util.nv.NvReadable;

public class StringUtils
{
	public static String emptyIfNull ( String s )
	{
		return s == null ? "" : s;
	}

	/**
	 * Return s, up to the first occurrence of delim. If delim does not occur, s
	 * is returned in full.
	 * 
	 * @param s original string
	 * @param delim the delimiter
	 * @return s, up to the first occurrence of delim
	 */
	public static String substringTo ( String s, char delim )
	{
		final int found = s.indexOf ( delim );
		return ( found > -1 ) ? s.substring ( 0, found ) : s;
	}

	public static int isOneOf ( char c, char[] set )
	{
		int result = -1;
		for ( result = 0; result < set.length && c != set[result]; result++ )
		{
		}
		return result >= set.length ? -1 : result;
	}

	public static int indexOfAnyOf ( String s, char[] chars )
	{
		return indexOfAnyOf ( s, chars, 0 );
	}

	public static int indexOfAnyOf ( String s, char[] chars, int fromIndex )
	{
		int result = -1;
		for ( int i = fromIndex; i < s.length () && result == -1; i++ )
		{
			final int one = isOneOf ( s.charAt ( i ), chars );
			if ( -1 != one )
			{
				result = i;
			}
		}
		return result;
	}

	public static String toFirstUpperRestLower ( String s )
	{
		return toFirstUpper ( s == null ? null : s.toLowerCase () );
	}

	public static String toFirstUpper ( String s )
	{
		if ( s == null )
			return s;

		final int len = s.length ();
		if ( len == 0 )
		{
			return s;
		}

		final StringBuffer sb = new StringBuffer ();
		sb.append ( Character.toUpperCase ( s.charAt ( 0 ) ) );
		if ( len > 1 )
		{
			sb.append ( s.substring ( 1 ) );
		}
		return sb.toString ();
	}

	/**
	 * Remove surrounding quotes, either double quote or single quote, if one occurs
	 * on both ends of the string. This method does *not* deal with escaped internal quotes.
	 * @param s
	 * @return an unquoted string
	 */
	public static String dequote ( String s )
	{
		return dequote ( s, new char [] { '"', '\'' } );
	}

	/**
	 * Remove surrounding characters, if one occurs on both ends of the string.
	 * This method does *not* deal with escaped internal quotes.
	 * @param s
	 * @param quoteChars chars considered quotes
	 * @return an unquoted string
	 */
	public static String dequote ( String s, char[] quoteChars )
	{
		String result = s;
		if ( indexOfAnyOf ( s, quoteChars ) == 0 )
		{
			final char leading = s.charAt ( 0 );
			if ( s.charAt ( s.length () - 1 ) == leading )
			{
				result = s.substring ( 1, s.length () - 1 );
			}
		}
		return result;
	}

	public static String[] splitList ( String s )
	{
		final List<String> resultList = splitListToList ( s );
		return resultList.toArray ( new String [resultList.size ()] );
	}

	public static String[] splitList ( String s, char[] separators, char[] quoteChars )
	{
		final List<String> resultList = splitListToList ( s, separators, quoteChars );
		return resultList.toArray ( new String [resultList.size ()] );
	}

	public static List<String> splitListToList ( String s )
	{
		return splitListToList ( s, new char [] { ',', ';' }, new char [] { '\'', '"' } );
	}

	public static List<String> splitListToList ( String s, char[] separators, char[] quoteChars )
	{
		final LinkedList<String> resultList = new LinkedList<String> ();

		String remains = s;
		while ( remains.length () > 0 )
		{
			valueInfo vi = getLeadingValue ( remains, quoteChars, separators );
			if ( vi != null )
			{
				if ( vi.fValue == null )
				{
					vi = new valueInfo ( "", vi.fNextFieldAt );
				}

				resultList.add ( vi.fValue.trim () );
				if ( vi.fNextFieldAt > -1 )
				{
					remains = remains.substring ( vi.fNextFieldAt ).trim ();
				}
				else
				{
					remains = "";
				}
			}
		}
		return resultList;
	}

	public static class valueInfo
	{
		public valueInfo ()
		{
			this ( null, -1 );
		}

		public valueInfo ( String val, int next )
		{
			fValue = val;
			fNextFieldAt = next;
		}

		public final String fValue;
		public final int fNextFieldAt;
	}

	public static valueInfo getLeadingValue ( String from )
	{
		return getLeadingValue ( from, '\"', ',' );
	}

	public static valueInfo getLeadingValue ( String from, char quoteChar, char delimChar )
	{
		return getLeadingValue ( from, new char [] { quoteChar }, new char [] { delimChar } );
	}

	public static valueInfo getLeadingValue ( String from, char[] quoteChars, char[] delimChars )
	{
		valueInfo vi = new valueInfo ();
		if ( from.length () > 0 )
		{
			char current = from.charAt ( 0 );
			final int quoteId = isOneOf ( current, quoteChars );
			boolean quoted = ( quoteId != -1 );

			if ( quoted )
			{
				final char quoteChar = quoteChars[quoteId];

				// scan for close quote
				int foundEnd = -1;
				int quoteScanFrom = 1;
				while ( -1 == foundEnd )
				{
					int quote = from.indexOf ( quoteChar, quoteScanFrom );
					if ( quote == -1 )
					{
						// improper format!
						break;
					}
					else
					{
						// check if this is a double quote inside the string or
						// if this quote terminates the field
						if ( quote + 1 < from.length () && from.charAt ( quote + 1 ) == quoteChar )
						{
							quoteScanFrom = quote + 2;
						}
						else
						{
							foundEnd = quote;
						}
					}
				}
				if ( foundEnd > -1 )
				{
					StringBuffer fixedUp = new StringBuffer ();
					String val = from.substring ( 1, foundEnd );
					boolean lastWasQuote = false;
					for ( int i = 0; i < val.length (); i++ )
					{
						char c = val.charAt ( i );
						if ( c == quoteChar )
						{
							if ( !lastWasQuote )
							{
								fixedUp.append ( c );
							}
							// else: drop it
							lastWasQuote = !lastWasQuote;
						}
						else
						{
							fixedUp.append ( c );
							lastWasQuote = false;
						}
					}

					final int nextFieldAt = indexOfAnyOf ( from, delimChars, foundEnd + 1 );
					vi = new valueInfo ( fixedUp.toString (), nextFieldAt != -1 ? nextFieldAt + 1 : nextFieldAt );
				}
			}
			else
			{
				// scan for delimiter
				int delim = indexOfAnyOf ( from, delimChars );
				if ( delim == -1 )
				{
					vi = new valueInfo ( from, -1 );
				}
				else
				{
					if ( delim == 0 )
					{
						vi = new valueInfo ( null, 1 );
					}
					else
					{
						vi = new valueInfo ( from.substring ( 0, delim ), delim + 1 );
					}
				}
			}
		}
		return vi;
	}

	public static class fieldInfo
	{
		public fieldInfo ( String val, int startPos )
		{
			fValue = val;
			fStartsAt = startPos;
		}

		public String toString ()
		{
			return fValue + " [" + fStartsAt + "]";
		}

		public final String fValue;
		public final int fStartsAt;
	}

	public static List<fieldInfo> split ( String from, char quoteChar, char delimChar )
	{
		final LinkedList<fieldInfo> result = new LinkedList<fieldInfo> ();

		String remains = from;
		int pos = 0;
		while ( remains.length () > 0 )
		{
			final valueInfo vi = getLeadingValue ( remains, quoteChar, delimChar );

			final fieldInfo fi = new fieldInfo ( vi.fValue, pos );
			result.add ( fi );

			if ( vi.fNextFieldAt > -1 )
			{
				pos += vi.fNextFieldAt;
				remains = remains.substring ( vi.fNextFieldAt );
			}
			else
			{
				remains = "";
			}
		}

		return result;
	}

	public interface charSelector
	{
		boolean select ( Character c );
	}

	public static int indexOf ( String s, charSelector cc )
	{
		final int len = s.length ();
		int current = 0;
		while ( current < len )
		{
			final Character currChar = s.charAt ( current );
			if ( cc.select ( currChar ) )
			{
				return current;
			}
			current++;
		}

		return -1;
	}

	public static String evaluate ( NvReadable data, String val )
	{
		if ( !val.contains ( "${" ) )
		{
			return val;
		}

		final StringBuffer sb = new StringBuffer ();

		String remains = val;
		while ( remains.length () > 0 )
		{
			final int dollar = remains.indexOf ( "${" );
			if ( dollar > -1 )
			{
				// there's no escaping here -- it's a symbol
				final int closeBrace = remains.indexOf ( "}", dollar + 2 );
				if ( closeBrace > -1 )
				{
					if ( dollar > 0 )
					{
						sb.append ( remains.substring ( 0, dollar ) );
					}
					final String symbol = remains.substring ( dollar + 2, closeBrace );

					final String replacement = data.getString ( symbol, "" );
					sb.append ( replacement );
					remains = remains.substring ( closeBrace + 1 );
				}
			}
			else
			{
				sb.append ( remains );
				remains = "";
			}
		}

		return sb.toString ();
	}
}
