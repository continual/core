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

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Sink;

public class FileSink implements Sink
{
	public static final String stdout = "stdout";
	public static final String stderr = "stderr";
	
	public FileSink () throws BuildFailure
	{
		this ( new JSONObject () );
	}

	public FileSink ( String stream ) throws BuildFailure
	{
		this ( new JSONObject ().put ( "to", stream ) );
	}

	public FileSink ( String stream, String lineFormat ) throws BuildFailure
	{
		this ( new JSONObject ()
			.put ( "to", stream )
			.put ( "lineFormat", lineFormat )
		);
	}

	public FileSink ( JSONObject config ) throws BuildFailure
	{
		this ( null, config );
	}

	public FileSink ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
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
			try
			{
				fStream = new PrintStream (
					new FileOutputStream (
						new File ( to ),
						config.optBoolean ( "append", true )
					)
				);
				fCloseStream = true;
			}
			catch ( FileNotFoundException e )
			{
				throw new BuildFailure ( e );
			}
		}

		fLineFormat = config.optString ( "lineFormat", null );

		fHeader = config.optString ( "header", null );
		fFooter = config.optString ( "footer", null );
	}

	@Override
	public void init ()
	{
		if ( fHeader != null )
		{
			fStream.println ( fHeader );
		}
	}

	@Override
	public void close ()
	{
		if ( fFooter != null )
		{
			fStream.println ( fFooter );
		}

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
		final String line = fLineFormat == null ?
			context.getMessage ().toLine () :
			context.evalExpression ( fLineFormat ) 
		;
		fStream.println ( line );
	}

	private final PrintStream fStream;
	private final boolean fCloseStream;
	private final String fLineFormat;
	private final String fHeader;
	private final String fFooter;
}
