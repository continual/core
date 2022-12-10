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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.junit.Test;

import io.continual.util.data.csv.CsvCallbackReader.RecordHandler;

public class CsvCallbackReaderTest
{
	@Test
	public void testBrokenLine () throws Exception
	{
		final ByteArrayInputStream is = new ByteArrayInputStream (
			(
				"FieldA,FieldB\n" + 
				"\"a\",\"b\n" +
				"c\"\n" +
				"d,e"
			).getBytes () );

		final StringBuffer sb = new StringBuffer ();
		final CsvCallbackReader<Exception> reader = new CsvCallbackReader<Exception> ( true );
		reader.read ( is, new RecordHandler<Exception> ()
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
	public void testBrokenHeaderLine () throws IOException, Exception
	{
		final ByteArrayInputStream is = new ByteArrayInputStream (
			(
				"FieldA,FieldB,\"Field\nC\"\n" +
				"\"a\",\"b\",\"c\"\n" +
				"\"d\",\"e\",\"f\"\n"
			).getBytes () );

		final StringBuffer sb = new StringBuffer ();
		final CsvCallbackReader<Exception> reader = new CsvCallbackReader<Exception> ( true );
		reader.read ( is, new RecordHandler<Exception> ()
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
			final CsvCallbackReader<Exception> r = new CsvCallbackReader<Exception> ( false );
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
		{ ",test2", "" },
		{ " \",test2\"", " \"" },
	};
	
	private RecordHandler<Exception> myRecordHandler = new RecordHandler<Exception> ()
	{
		@Override
		public boolean handler ( Map<String, String> fields )
		{
			String field = fields.get ( "FieldA" );
			return field==null ? true : false;
		}
	};
	
	@Test(expected = IOException.class)
	public void readException () throws Exception {
		CsvCallbackReader<Exception> reader = new CsvCallbackReader<Exception> ( true );
		reader.read((InputStream)null, null);
	}
	
	@Test
	public void read () throws Exception {
		CsvCallbackReader<Exception> reader = new CsvCallbackReader<Exception> ( true );
		InputStreamReader inputStream = new InputStreamReader ( new ByteArrayInputStream ( "header\ntest2".getBytes () ) );
		reader.read(inputStream, myRecordHandler);
		assertEquals ( true, reader.hasHeader());
		assertEquals (1, reader.getLinesParsed());
		assertEquals ("header", reader.getColumnNames().get(0));
	}
	
	@Test
	public void reset () throws Exception {
		CsvCallbackReader<Exception> reader = new CsvCallbackReader<Exception> ( true );
		InputStreamReader inputStream = new InputStreamReader ( new ByteArrayInputStream ( "header \n first \n second \n third".getBytes () ) );
		reader.read(inputStream, myRecordHandler);
		assertEquals (3, reader.getLinesParsed());
		reader.reset();
		assertEquals (0, reader.getLinesParsed());
	}
	
	@Test
	public void resad_header_with_number_sign () throws Exception {
		CsvCallbackReader<Exception> reader = new CsvCallbackReader<Exception> ( true );
		InputStreamReader inputStream = new InputStreamReader ( new ByteArrayInputStream ( "#header \n first line \n second \n third".getBytes () ) );
		reader.read(inputStream, myRecordHandler);
		assertEquals (2, reader.getLinesParsed());
		assertEquals ("first line", reader.getColumnNames().get(0));
	}
	
	@Test
	public void read_empty_header () throws Exception {
		CsvCallbackReader<Exception> reader = new CsvCallbackReader<Exception> ( true );
		InputStreamReader inputStream = new InputStreamReader ( new ByteArrayInputStream ( "\n first line \n second \n third".getBytes () ) );
		reader.read(inputStream, myRecordHandler);
		assertEquals (2, reader.getLinesParsed());
		assertEquals ("first line", reader.getColumnNames().get(0));
	}
	
	@Test
	public void read_no_header() throws Exception {
		CsvCallbackReader<Exception> reader = new CsvCallbackReader<Exception> ( false );
		InputStreamReader inputStream = new InputStreamReader ( new ByteArrayInputStream ( "1st line\n 2nd line,other".getBytes () ) );
		reader.read(inputStream, myRecordHandler);
		assertEquals (2, reader.getLinesParsed());
	}
}
