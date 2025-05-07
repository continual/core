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

package io.continual.services.processor.engine.library.sinks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import io.continual.services.processor.engine.model.MessageProcessingContext;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.Sink;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.csv.CsvLineBuilder;
import io.continual.util.data.json.JsonEval;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayOfObjectVisitor;

public class CsvSink implements Sink
{
	public static final String stdout = "stdout";
	public static final String stderr = "stderr";
	
	public static CsvSink toStdOut () throws BuildFailure
	{
		return new CsvSink ( stdout );
	}
	
	public CsvSink () throws BuildFailure
	{
		this ( new JSONObject () );
	}

	public CsvSink ( String stream ) throws BuildFailure
	{
		this ( new JSONObject ().put ( "to", stream ) );
	}

	public CsvSink ( JSONObject config ) throws BuildFailure
	{
		this ( null, config );
	}

	public CsvSink ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		try
		{
			final String to = config.optString ( "to", stdout );
			if ( to.equals ( stdout ) )
			{
				fStream = System.out;
				fCloseStream = false;
			}
			else if ( to.equals ( stderr ) )
			{
				fStream = System.err;
				fCloseStream = false;
			}
			else
			{
				fStream = new PrintStream ( new FileOutputStream ( new File ( to ) ) );
				fCloseStream = true;
			}

			// The columns are expressed as an array of objects to preserve order.
			// The objects contain the column header, the evaluation expression, and 
			// any additional metadata like target type.

			fCols = new ArrayList<> ();
			JsonVisitor.forEachObjectIn ( config.optJSONArray ( "columns" ), new ArrayOfObjectVisitor () {
				@Override
				public boolean visit ( JSONObject col ) throws JSONException
				{
					fCols.add ( new ColInfo ( col ) );
					return true;
				}
			} );

			// hasOutputHeader = false will generate a header
			final boolean wantHeader = config.optBoolean ( "outputHeader", true );
			fHasOutputHeader = !wantHeader;
		}
		catch ( FileNotFoundException | JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	public CsvSink addColumn ( String key, String value )
	{
		return addColumn ( key, value, String.class );
	}

	public CsvSink addColumn ( String key, String value, Class<?> clazz )
	{
		fCols.add ( new ColInfo ( key, value, clazz ) );
		return this;
	}

	@Override
	public void init ()
	{
	}

	@Override
	public void close ()
	{
		if ( fCloseStream )
		{
			fStream.close ();
		}
	}

	@Override
	public void flush ()
	{
		fStream.flush ();
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		if ( !fHasOutputHeader )
		{
			// output the header line
			final CsvLineBuilder clb = new CsvLineBuilder ();
			for ( ColInfo ci : fCols )
			{
				clb.append ( ci.getKey() );
			}
			fStream.println ( clb.toString () );
			fHasOutputHeader = true;
		}

		final JSONObject msgJson = context.getMessage ().toJson ();

		// output the data line
		final CsvLineBuilder clb = new CsvLineBuilder ();
		for ( ColInfo ci : fCols )
		{
			final String val = JsonEval.substitute ( ci.getExpr(), msgJson );
			final Class<?> targetClass = ci.getTargetClass ();
			try
			{
				if ( targetClass.equals ( Integer.class ) )
				{
					if ( val.isEmpty () )
					{
						clb.appendEmpty ();
					}
					else
					{
						clb.append ( Integer.parseInt ( val ) );
					}
				}
				else if ( targetClass.equals ( Long.class ) )
				{
					if ( val.isEmpty () )
					{
						clb.appendEmpty ();
					}
					else
					{
						clb.append ( Long.parseLong ( val ) );
					}
				}
				else if ( targetClass.equals ( Double.class ) )
				{
					if ( val.isEmpty () )
					{
						clb.appendEmpty ();
					}
					else
					{
						clb.append ( Double.parseDouble ( val ) );
					}
				}
				else if ( targetClass.equals ( Boolean.class ) )
				{
					clb.append ( TypeConvertor.convertToBooleanBroad ( val ) );
				}
				else
				{
					clb.append ( val );
				}
			}
			catch ( NumberFormatException e )
			{
				clb.append ( val );
			}
		}
		fStream.println ( clb.toString () );
	}

	private final PrintStream fStream;
	private final boolean fCloseStream;
	private final ArrayList<ColInfo> fCols;
	private boolean fHasOutputHeader;

	private static class ColInfo
	{
		public ColInfo ( String key, String expr, Class<?> clazz )
		{
			fKey = key;
			fExpr = expr;
			fClass = clazz;
		}

		public ColInfo ( JSONObject data )
		{
			this ( data.getString ( "key" ), data.getString ( "expr" ), classFrom ( data.optString ( "type", null ) ) );
		}

		public String getKey () { return fKey; }
		public String getExpr () { return fExpr; }
		public Class<?> getTargetClass () { return fClass; }

		private final String fKey;
		private final String fExpr;
		private final Class<?> fClass;

		private static Class<?> classFrom ( String text )
		{
			if ( text == null || text.isEmpty () ) return String.class;

			text = text.trim ().toLowerCase ();
			if ( text.startsWith ( "int" ) )
			{
				return Integer.class;
			}
			else if ( text.startsWith ( "long" ) )
			{
				return Long.class;
			}
			else if ( text.startsWith ( "double" ) )
			{
				return Double.class;
			}
			else if ( text.startsWith ( "bool" ) )
			{
				return Boolean.class;
			}
			// etc....

			return String.class;
		}
	}
}
