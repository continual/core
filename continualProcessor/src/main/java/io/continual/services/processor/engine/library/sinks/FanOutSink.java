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

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Sink;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;

public class FanOutSink implements Sink
{
	public FanOutSink ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fSinks = new ArrayList<> ();
		try
		{
			JsonVisitor.forEachElement ( config.optJSONArray ( "sinks" ), new ArrayVisitor<JSONObject,BuildFailure> () {
	
				@Override
				public boolean visit ( JSONObject sink ) throws JSONException,BuildFailure
				{
					fSinks.add (
						Builder.withBaseClass ( Sink.class )
							.providingContext ( sc )
							.withClassNameInData ()
							.usingData ( sink )
							.build ()
						);
					return true;
				}
				
			} );
		}
		catch ( JSONException x )
		{
			throw new BuildFailure ( x );
		}
	}

	@Override
	public void init ( )
	{
		for ( Sink s : fSinks )
		{
			s.init ( );
		}
	}

	@Override
	public void close ()
	{
		for ( Sink s : fSinks )
		{
			try
			{
				s.close ();
			}
			catch ( IOException e )
			{
				log.error ( e.getMessage () );
			}
		}
	}

	@Override
	public void flush ()
	{
		for ( Sink s : fSinks )
		{
			s.flush ();
		}
	}

	@Override
	public void process ( Message msg )
	{
		// no-op
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		for ( Sink s : fSinks )
		{
			s.process ( context );
		}
	}


	private final ArrayList<Sink> fSinks;

	private static final Logger log = LoggerFactory.getLogger ( FanOutSink.class );
}
