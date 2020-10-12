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

import java.io.PrintStream;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.Sink;

public class ConsoleSink implements Sink
{
	public static final String stdout = "stdout";
	public static final String stderr = "stderr";
	
	public ConsoleSink () throws BuildFailure
	{
		this ( new JSONObject () );
	}

	public ConsoleSink ( String stream ) throws BuildFailure
	{
		this ( new JSONObject ().put ( "to", stream ) );
	}

	public ConsoleSink ( JSONObject config ) throws BuildFailure
	{
		this ( null, config );
	}

	public ConsoleSink ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		final String to = config.optString ( "to", stdout );
		if ( to.equals ( stdout ) )
		{
			fStream = System.out;
		}
		else if ( to.equals ( stderr ) )
		{
			fStream = System.err;
		}
		else
		{
			throw new BuildFailure ( "ConsoleSink requires to=" + stdout + " or " + stderr );
		}
	}

	@Override
	public void init ()
	{
	}

	@Override
	public void close ()
	{
	}

	@Override
	public void flush ()
	{
	}

	@Override
	public void process ( Message msg )
	{
		fStream.println ( msg.toLine () );
	}

	private final PrintStream fStream;
}
