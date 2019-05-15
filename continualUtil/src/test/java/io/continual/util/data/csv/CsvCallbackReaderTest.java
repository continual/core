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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;

import io.continual.util.data.csv.CsvCallbackReader.recordHandler;

public class CsvCallbackReaderTest extends TestCase
{
	@Test
	public void testBrokenLine () throws IOException
	{
		final ByteArrayInputStream is = new ByteArrayInputStream (
			(
				"FieldA,FieldB\n" + 
				"\"a\",\"b\n" +
				"c\"\n" +
				"d,e"
			).getBytes () );

		final StringBuffer sb = new StringBuffer ();
		final CsvCallbackReader reader = new CsvCallbackReader( true );
		reader.read ( is, new recordHandler()
		{
			@Override
			public boolean handler ( Map<String, String> fields )
			{
				sb.append ( fields.get ( "FieldA" ) );
				return true;
			}
		} );
		assertEquals ( "ad", sb.toString () );
	}

	@Test
	public void testBrokenHeaderLine () throws IOException
	{
		final ByteArrayInputStream is = new ByteArrayInputStream (
			(
				"FieldA,FieldB,\"Field\nC\"\n" +
				"\"a\",\"b\",\"c\"\n" +
				"\"d\",\"e\",\"f\"\n"
			).getBytes () );

		final StringBuffer sb = new StringBuffer ();
		final CsvCallbackReader reader = new CsvCallbackReader( true );
		reader.read ( is, new recordHandler()
		{
			@Override
			public boolean handler ( Map<String, String> fields )
			{
				sb.append ( fields.get ( "FieldA" ) );
				return true;
			}
		} );
		assertEquals ( "ad", sb.toString () );
	}

	@Test
	public void testTermReader () throws IOException
	{
		for ( String[] test : terms )
		{
			final CsvCallbackReader r = new CsvCallbackReader( false );
			r.readTerm ( new InputStreamReader ( new ByteArrayInputStream ( test[0].getBytes () ) ) );
			assertEquals ( test[1], r.fLastToken );
		}
	}

	private final String[][] terms = new String[][]
	{
		{ "test1,test2,test3", "test1" },
		{ "\"test1\",test2,test3", "test1" },
		{ "\"test1\na\",test2,test3", "test1\na" },
		{ "\"test1\"\"\",test2,test3", "test1\"" },
	};
}
