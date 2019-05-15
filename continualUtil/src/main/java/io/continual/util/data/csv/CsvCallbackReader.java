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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CsvCallbackReader
{
	public static final String kLineField = "line";
	public static final String kLineField_Default = "line";

	public static final String kHasHeaderRow = "header";
	public static final String kDelimiter = "delimiter";
	public static final String kQuote = "quote";
	public static final String kPassThru = "passthru";

	public interface recordHandler
	{
		/**
		 * handle a CSV line
		 * @param fields
		 * @return true to continue
		 */
		boolean handler ( Map<String,String> fields );
	}

	public CsvCallbackReader(boolean header )
	{
		this ( '"', ',', header );
	}

	public CsvCallbackReader(char quoteChar, char fieldSepChar, boolean header )
	{
		fDelimiter = fieldSepChar;
		fQuote = quoteChar;
		fColumns = null;
		fHasHeaderRow = header;
		fLineCount = 0;
	}

	public void reset ()
	{
		fLineCount = 0;
	}

	public void read ( InputStream is, recordHandler rh ) throws IOException
	{
		if ( is == null ) throw new IOException ( "No CSV stream provided" );
		final InputStreamReader isr = new InputStreamReader ( is, StandardCharsets.UTF_8 );
		read ( isr, rh );
	}

	public void read ( InputStreamReader isr, recordHandler rh ) throws IOException
	{
		boolean keepGoing = true;
		HashMap<String,String> s = readNextLine ( isr );
		while ( s != null && keepGoing )
		{
			keepGoing = rh.handler ( s );
			if ( keepGoing )
			{
				s = readNextLine ( isr );
			}
		}
		if ( s == null )
		{
			isr.close ();
		}
	}

	public List<String> getColumnNames ()
	{
		final LinkedList<String> result = new LinkedList<String> ();
		if ( fColumns != null )
		{
			for ( String c : fColumns )
			{
				result.add ( c );
			}
		}
		return result;
	}

	public int getLinesParsed ()
	{
		return fLineCount;
	}

	public boolean hasHeader ()
	{
		return fHasHeaderRow;
	}

	private int fLineCount;
	private final char fDelimiter;
	private final char fQuote;
	private String[] fColumns;
	private boolean fHasHeaderRow;

	String fLastToken;
	boolean fLastOnLine;
	
	protected void readTerm ( InputStreamReader is ) throws IOException
	{
		fLastOnLine = false;
		fLastToken = null;

		final StringBuffer sb = new StringBuffer ();

		int current = is.read ();
		if ( current < 0 || isLineEnding ( current ) )
		{
			fLastOnLine = true;
			return;
		}

		if ( current == fDelimiter )
		{
			fLastOnLine = false;
			fLastToken = "";
			return;
		}

		final boolean quoted = ( current == fQuote );
		if ( !quoted )
		{
			sb.append ( (char) current );
		}
		// else: ignore the quote

		// now read until the terminal character is seen. note that
		// the quote character is escaped by doubling it.

		boolean terminated = false;
		boolean lastWasQuote = false;
		while ( !terminated )
		{
			current = is.read ();
			if ( current < 0 )
			{
				// end of stream. return what we had.
				fLastToken = sb.toString ();
				fLastOnLine = true;
				return;
			}
	
			if ( current == fQuote )
			{
				if ( quoted )
				{
					if ( lastWasQuote )
					{
						sb.append ( (char) current );
						lastWasQuote = false;
					}
					else
					{
						lastWasQuote = true;
					}
				}
				else
				{
					sb.append ( (char) current );
				}
			}
			else if ( current == fDelimiter || isLineEnding ( current ) )
			{
				if ( quoted )
				{
					if ( lastWasQuote )
					{
						// the end
						terminated = true;
						fLastOnLine = isLineEnding ( current );
					}
					else
					{
						// just a delim/newline in the middle
						sb.append ( (char) current );
					}
				}
				else 
				{
					// when not quoting, this ends the term
					terminated = true;
					fLastOnLine = isLineEnding ( current );
				}
			}
			else
			{
				sb.append ( (char) current );
			}
		}
		fLastToken = sb.toString ();
	}

	private static boolean isLineEnding ( int c )
	{
		// FIXME: this is a "newline" which may or may not work based on the line ending
		// saved into the CSV stream
		return c == '\n';
	}

	private List<String> readLineValues ( InputStreamReader is, boolean withTrim ) throws IOException
	{
		final LinkedList<String> result = new LinkedList<String> ();
		do
		{
			readTerm ( is );
			if ( fLastToken != null )
			{
				result.add ( withTrim ? fLastToken.trim () : fLastToken );
			}
		}
		while ( !fLastOnLine );
		return result;
	}

	private HashMap<String,String> readNextLine ( InputStreamReader is ) throws IOException
	{
		if ( fHasHeaderRow && fLineCount == 0 )
		{
			// read the header line
			parseHeader ( is );
		}
		return parseLine ( is );
	}

	private void parseHeader ( InputStreamReader is ) throws IOException
	{
		List<String> headers = readLineValues ( is, true );
		
		// skip empty lines or those that start with "#"
		while ( headers.size () == 0 || headers.iterator ().next ().startsWith ( "#" ) )
		{
			headers = readLineValues ( is, true );
		}

		fColumns = headers.toArray ( new String [headers.size ()] );
	}

	/**
	 * Read the next line from the CSV, providing header values into a map.
	 * @param is
	 * @return null if the EOF is reached
	 * @throws IOException
	 */
	private HashMap<String,String> parseLine ( InputStreamReader is ) throws IOException
	{
		final HashMap<String,String> result = new HashMap<String,String> ();

		int colNum = 0;

		// a line that ends in an empty field can get parsed incorrectly (e.g. "foo,")
		// because the "remainder" after the first field is empty, looking just like
		// the usual end case. so, if we have a set of columns, set each value to the
		// empty string ahead of time.
		if ( fColumns != null )
		{
			for ( String col : fColumns )
			{
				result.put ( col, "" );
			}
		}

		boolean didOne = false;
		do
		{
			readTerm ( is );
			if ( fLastToken != null )
			{
				didOne = true;
				final String name = ( fColumns != null && colNum<fColumns.length ? fColumns[colNum] : "" + colNum );
				colNum++;
				result.put ( name, fLastToken );
			}
		}
		while ( !fLastOnLine );

		if ( didOne ) fLineCount++;

		return didOne ? result : null;
	}
}
