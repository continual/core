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

import java.util.Date;

public class CsvLineBuilder 
{
	public CsvLineBuilder ()
	{
		this ( '"', ',', true );
	}

	public CsvLineBuilder ( char quoteChar, char sepChar, boolean forceQuotes )
	{
		fBuilder = new StringBuilder ();
		fDoneOne = false;
		fQuoteChar = quoteChar;
		fSepChar = sepChar;
		fForceQuotes = forceQuotes;
	}

	@Override
	public String toString ()
	{
		return fBuilder.toString ();
	}

	public CsvLineBuilder append ( String value )
	{
		return appendLiteral ( CsvEncoder.encodeForCsv ( value, fQuoteChar, fSepChar, fForceQuotes ) );
	}

	public CsvLineBuilder append ( )
	{
		return append ( "" );
	}

	public CsvLineBuilder append ( long value )
	{
		return appendLiteral ( Long.toString ( value ) );
	}

	public CsvLineBuilder append ( boolean value )
	{
		return appendLiteral ( Boolean.toString ( value ) );
	}

	public CsvLineBuilder append ( double value )
	{
		return appendLiteral ( Double.toString ( value ) );
	}

	public CsvLineBuilder append ( Date value )
	{
		return appendLiteral ( CsvEncoder.encodeForCsv ( value ) );
	}

	private final StringBuilder fBuilder;
	private final char fQuoteChar;
	private final char fSepChar;
	private final boolean fForceQuotes;
	private boolean fDoneOne;

	public CsvLineBuilder appendLiteral ( String val )
	{
		if ( fDoneOne ) fBuilder.append ( ',' );
		fBuilder.append ( val );
		fDoneOne = true;
		return this;
	}
}
