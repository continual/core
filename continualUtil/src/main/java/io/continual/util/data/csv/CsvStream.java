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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import io.continual.util.data.StringUtils;
import io.continual.util.data.StringUtils.valueInfo;

public class CsvStream
{
	public static final String kLineField = "line";
	public static final String kLineField_Default = "line";

	public static final String kHasHeaderRow = "header";
	public static final String kDelimiter = "delimiter";
	public static final String kQuote = "quote";
	public static final String kPassThru = "passthru";

	public CsvStream(boolean header )
	{
		this ( CsvEncoder.kDefaultQuoteChar, CsvEncoder.kDefaultFieldSeparatorChar, header );
	}

	public CsvStream(char quoteChar, char fieldSepChar, boolean header )
	{
		fDelimiter = fieldSepChar;
		fQuote = quoteChar;
		fColumns = null;
		fHasHeaderRow = header;
		fLineCount = 0;
		fRecords = new ArrayList<HashMap<String,String>> ();
	}

	public void reset ()
	{
		fLineCount = 0;
		fRecords.clear ();
	}

	public void read ( InputStream is ) throws IOException
	{
		BufferedReader br = new BufferedReader ( new InputStreamReader ( is ) );
		read ( br );
	}

	public void read ( BufferedReader br ) throws IOException
	{
		String s;
		while ( (s = br.readLine ()) != null )
		{
			s = s.trim ();
			if ( fLineCount == 0 && fHasHeaderRow )
			{
				// deal with header
				parseHeader ( s );
			}
			else
			{
				// deal with regular lines
				final HashMap<String,String> rec = parseLine ( s );
				fRecords.add ( rec );
			}
			fLineCount++;
		}
	}

	public Vector<String> getColumnNames ()
	{
		final Vector<String> result = new Vector<String> ();
		for ( String c : fColumns )
		{
			result.add ( c );
		}
		return result;
	}

	public int getLinesParsed ()
	{
		return fLineCount;
	}

	public int getRecordCount ()
	{
		return fRecords.size ();
	}

	/**
	 * Get a record by row number. Note that the returned map can include nulls for values.
	 * @param row
	 * @return
	 */
	public Map<String,String> getRecord ( int row )
	{
		return fRecords.get ( row );
	}

	public String getField ( int row, int field )
	{
		final String name = ( fColumns != null && field<fColumns.length ? fColumns[field] : "" + field );
		return getRecord ( row ).get ( name );
	}

	private int fLineCount;
	private final char fDelimiter;
	private final char fQuote;
	private String[] fColumns;
	private boolean fHasHeaderRow;
	private final ArrayList<HashMap<String,String>> fRecords;

	public static List<String> parseHeaderLine ( String line, char quote, char delim )
	{
		final LinkedList<String> headers = new LinkedList<String> ();

		String remains = line;
		while ( remains.length () > 0 )
		{
			valueInfo result = StringUtils.getLeadingValue ( remains, quote, delim );
			if ( result != null )
			{
				if ( result.fValue == null )
				{
					// odd... an empty value in the column header line
					result = new valueInfo ( "unnamed-" + (headers.size()+1), result.fNextFieldAt );
				}

				headers.add ( result.fValue );
				if ( result.fNextFieldAt > -1 )
				{
					remains = remains.substring ( result.fNextFieldAt );
				}
				else
				{
					remains = "";
				}
			}
		}
		return headers;
	}

	private void parseHeader ( String line )
	{
		LinkedList<String> headers = new LinkedList<String> ();

		String remains = line;
		while ( remains.length () > 0 )
		{
			valueInfo result = StringUtils.getLeadingValue ( remains, fQuote, fDelimiter );
			if ( result != null )
			{
				if ( result.fValue == null )
				{
					// odd... an empty value in the column header line
					result = new valueInfo ( "unnamed-" + (headers.size()+1), result.fNextFieldAt );
				}

				headers.add ( result.fValue );
				if ( result.fNextFieldAt > -1 )
				{
					remains = remains.substring ( result.fNextFieldAt );
				}
				else
				{
					remains = "";
				}
			}
		}
		fColumns = headers.toArray ( new String [headers.size ()] );
	}

	protected HashMap<String,String> parseLine ( String line )
	{
		final HashMap<String,String> result = new HashMap<String,String> ();

		int colNum = 0;
		String remains = new String ( line );
		while ( remains != null )
		{
			final StringUtils.valueInfo vi = StringUtils.getLeadingValue ( remains, fQuote, fDelimiter );
			final String name = ( fColumns != null && colNum<fColumns.length ? fColumns[colNum] : "" + colNum );
			colNum++;
			result.put ( name, vi.fValue );
			if ( vi.fNextFieldAt > -1 )
			{
				remains = remains.substring ( vi.fNextFieldAt );
			}
			else
			{
				remains = null;
			}
		}

		return result;
	}
}
