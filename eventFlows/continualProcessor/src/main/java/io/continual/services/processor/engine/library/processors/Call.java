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

package io.continual.services.processor.engine.library.processors;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Pipeline;
import io.continual.services.processor.engine.model.Processor;
import io.continual.services.processor.engine.model.Program;
import io.continual.util.data.json.JsonVisitor;

public class Call implements Processor
{
	public Call ( String... pipelines )
	{
		fPipelines = Arrays.asList ( pipelines );
	}

	public Call ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		try
		{
			fPipelines = new LinkedList<> ();

			final Object o = config.opt ( "to" );
			if ( o instanceof String )
			{
				fPipelines.add ( o.toString () );
			}
			else if ( o instanceof JSONArray )
			{
				fPipelines.addAll ( JsonVisitor.arrayToList ( (JSONArray) o ) );
			}
			else
			{
				throw new BuildFailure ( "Call requires a 'to' value that's either a string or an array of strings." );
			}
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		final Program p = context.getStreamProcessingContext ().getProgram ();

		for ( String pn : fPipelines )
		{
			final Pipeline pl = p.getPipeline ( pn );
			if ( pl == null )
			{
				context.stopProcessing ( "Pipeline " + pn + " is not in the program." );
				return;
			}
			
			
			pl.process ( context );
		}
	}

	private final List<String> fPipelines;
}
