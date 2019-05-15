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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import io.continual.util.data.StringUtils;
import io.continual.util.data.TypeConvertor;

public class CsvInspector
{
	public CsvInspector()
	{
		fLines = new Vector<String> ();
		fHeader = false;
		fQuoteChar = kStdQuotes[0];
		fDelimChar = kStdDelims[0];
	}

	public void setHints ( boolean header, char dc, char qc )
	{
		fHeader = header;
		fQuoteChar = qc;
		fDelimChar = dc;
	}

	public void reevaluate ()
	{
		parseLines ();
		inspectTypes ();
	}

	/**
	 * examine some lines. the call will return true if the inspector has 
	 * a reasonable idea of what the format looks like. If false, there's no
	 * need to call other methods. 
	 * @param is
	 * @return true if the input sample had enough data
	 */
	public boolean readStreamForSample ( InputStream is )
	{
		LinkedList<String> list = new LinkedList<String> ();
		final BufferedReader br = new BufferedReader ( new InputStreamReader ( is ) );

		// read lines to get an idea of the format
		for ( int i=0; i<10; i++ )
		{
			try
			{
				final String line = br.readLine ();
				if ( line != null )
				{
					list.add ( line );
				}
			}
			catch ( IOException e )
			{
				break;
			}
		}

		return inputSample ( list );
	}

	public boolean inputSample ( String[] lines )
	{
		final LinkedList<String> list = new LinkedList<String> ();
		for ( String line : lines )
		{
			list.add ( line );
		}
		return inputSample ( list );
	}
	
	/**
	 * examine some lines. the call will return true if the inspector has 
	 * a reasonable idea of what the format looks like. If false, there's no
	 * need to call other methods. 
	 * @param lines
	 * @return true if the input sample had enough data
	 */
	public boolean inputSample ( List<String> lines )
	{
		boolean decent = false;

		if ( lines.size () >= 3 )
		{
			fLines = new Vector<String> ();
			final Iterator<String> it = lines.iterator ();
			for ( int i=0; i<5 && it.hasNext (); i++ )
			{
				fLines.add ( it.next () );
			}

			decent = detectSpecialChars () && detectHeader ();
		}

		return decent;
	}

	public boolean hasHeaderLine ()
	{
		return fHeader;
	}

	public char getQuoteChar ()
	{
		return fQuoteChar;
	}

	public char getDelimiterChar ()
	{
		return fDelimChar;
	}

	public int getFieldCount ()
	{
		return fTypes.length;
	}

	public int getRowCount ()
	{
		return fLines.size ();
	}

	public int getTypePossibilities ( int field )
	{
		return fTypes[field];
	}

	public String getLine ( int i )
	{
		return fLines.elementAt ( i );
	}

	public Vector<fieldInfo> getLineInfo ( int i )
	{
		return fFields.elementAt ( i );
	}

	public static final int kTypeString = 1;
	public static final int kTypeNumeric = 2;
	public static final int kTypeDate = 4;

	public static class fieldInfo
	{
		public String value () { return fValue; }
		public String fValue;
		public int fTypeMask;
	}

	private Vector<String> fLines;
	private boolean fHeader;
	private char fQuoteChar;
	private char fDelimChar;

	private int[] fTypes;
	private Vector<Vector<fieldInfo>> fFields;

	private static final char[] kStdDelims = { ',', '|', 0x09 };
	private static final char[] kStdQuotes = { '"', '\'' };
	private static final SimpleDateFormat[] kDateFormatters =
	{
		new SimpleDateFormat ( "MM/dd/yyyy" ),
		new SimpleDateFormat ( "MM/dd/yy" ),
		new SimpleDateFormat ( "MM-dd-yyyy" ),
		new SimpleDateFormat ( "MM-dd-yy" ),
		new SimpleDateFormat ( "yyyy-MM-dd" ),
		new SimpleDateFormat ( "yy-MM-dd" ),
	};

	private boolean detectSpecialChars ()
	{
		double best = 0.0;
		for ( char d : kStdDelims )
		{
			for ( char q : kStdQuotes )
			{
				final double trial = tryParse ( d, q );
				if ( trial > best )
				{
					fQuoteChar = q;
					fDelimChar = d;
					best = trial;
				}
			}
		}
		return best > 0.0;
	}

	private double tryParse ( char d, char q )
	{
		// this method rates a parsing choice with some heuristics about the
		// number of fields parsed, and how normal the format looks -- for example,
		// if you have the quote char around every text field, that's typical,
		// but if you don't, and you have quotes around a text field that contains
		// the separator, that's a strong suggestion that it's a correct choice.
		
		// we're looking for how many fields are parsed out given these settings

		int total = 0;
		int lines = 0;

		for ( String line : fLines )
		{
			if ( line.length () == 0 )
			{
				// ignore this line
				continue;
			}

			final List<StringUtils.fieldInfo> values = StringUtils.split ( line, q, d );

//			final List<String> values = CsvStream.parseHeaderLine ( line, q, d );
//			final int fieldCount = values.size ();
//			
//			final int thisLineCount = CsvStream.parseHeaderLine ( line, q, d ).size ();
			total += values.size ();
			lines++;
		}
		return ((double)total) / (double)lines;
	}

	private int detectType ( String value )
	{
		int result = kTypeString;

		// try numerics
		try
		{
			TypeConvertor.convertToDouble ( value );
			result |= kTypeNumeric;
		}
		catch ( TypeConvertor.conversionError e )
		{
			// ignore
		}

		// try date formats
		for ( SimpleDateFormat df : kDateFormatters )
		{
			try
			{
				df.parse ( value );
				result |= kTypeDate;
				break;
			}
			catch ( ParseException e )
			{
				// ignore
			}
		}
		
		return result;
	}

	private void parseLines ()
	{
		fFields = new Vector<Vector<fieldInfo>> ();

		int lineNo = -1;
		for ( String line : fLines )
		{
			lineNo++;

			final Vector<fieldInfo> fieldSet = new Vector<fieldInfo> ();
			fFields.insertElementAt ( fieldSet, lineNo );

			final List<String> values = CsvStream.parseHeaderLine ( line, fQuoteChar, fDelimChar );
			int colNo = -1;
			for ( String value : values )
			{
				colNo++;

				final fieldInfo fi = new fieldInfo ();
				fi.fValue = value;
				fi.fTypeMask = detectType ( value );
				fieldSet.insertElementAt ( fi, colNo );
			}
		}
	}

	private void inspectTypes ()
	{
		final int count = fFields.elementAt(1).size();
		fTypes = new int [ count ];
		for ( int i=0; i<count; i++ )
		{
			fTypes[i] = kTypeString | kTypeNumeric | kTypeDate;
		}

		// get an understanding of types present in data lines
		for ( int i=1; i<fFields.size(); i++ )
		{
			final Vector<fieldInfo> row = fFields.elementAt ( i );
			for ( int j=0; j<count; j++ )
			{
				if ( row.size () > j )
				{
					final fieldInfo field = row.elementAt ( j );
					final int fieldType = field.fTypeMask;
					final int anded = fieldType & fTypes[j];
					fTypes[j] = anded;
				}
				// else: no such value on this line, skip it
			}
		}
	}

	private boolean detectHeader ()
	{
		parseLines ();
		inspectTypes ();

		// now compare to first line
		boolean same = true;
		final Vector<fieldInfo> row = fFields.elementAt ( 0 );
		for ( int j=0; j<fTypes.length; j++ )
		{
			if ( row.size () > j )
			{
				final fieldInfo field = row.elementAt ( j );
				final int fieldType = field.fTypeMask;
				final int colDataType = fTypes[j];
				final int mask = colDataType & fieldType;
				same = same && ( mask == colDataType );
				if ( !same ) break;
			}
		}

		fHeader = !same;

		return true;
	}
}
