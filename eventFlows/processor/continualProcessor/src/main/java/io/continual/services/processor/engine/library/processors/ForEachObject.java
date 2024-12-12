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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.config.readers.ConfigReadException;
import io.continual.services.processor.config.readers.JsonConfigReader;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Pipeline;
import io.continual.services.processor.engine.model.Processor;
import io.continual.util.data.json.JsonEval;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

public class ForEachObject implements Processor
{
	public ForEachObject ( ConfigLoadContext clc, JSONObject config ) throws BuildFailure
	{
		try
		{
			fSet = config.getString ( "set" );
			fVarName = config.optString ( "varName", "_item" );
			
			final Object processing = config.opt ( "processing" );
			if ( processing instanceof JSONArray )
			{
				fPipelineRef = null;
				fPipeline = JsonConfigReader
					.readPipeline ( (JSONArray) processing, new ArrayList<String>(), clc )
				;
			}
			else if ( processing instanceof String )
			{
				fPipelineRef = processing.toString ();
				fPipeline = null;
			}
			else
			{
				throw new BuildFailure ( "Invalid processing configuration" );
			}
		}
		catch ( JSONException | ConfigReadException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		final JSONObject data = context.getMessage ().accessRawJson ();
		final Object setData = JsonEval.eval ( data, fSet );
		if ( setData instanceof JSONArray )
		{
			JsonVisitor.forEachElement ( (JSONArray)setData, new ArrayVisitor<JSONObject,JSONException> ()
			{
				@Override
				public boolean visit ( JSONObject itemElement ) throws JSONException
				{
					final Object valWas = context.getMessage ().getRawValue ( fVarName );

					context.getMessage ()
						.putRawValue ( fVarName, itemElement )
					;

					runProcessing ( context );

					context.getMessage ()
						.clearValue ( fVarName )
						.putRawValue ( fVarName, valWas )
					;
					return true;
				}
			} );
		}
		else if ( setData instanceof JSONObject )
		{
			JsonVisitor.forEachElement ( (JSONObject)setData, new ObjectVisitor<Object,JSONException> ()
			{
				@Override
				public boolean visit ( String key, Object itemElement ) throws JSONException
				{
					if ( itemElement instanceof JSONObject || itemElement instanceof String )
					{
						context.getMessage ()
							.putRawValue ( "_key", key )
							.putRawValue ( "_item", itemElement )
						;
	
						runProcessing ( context );
	
						context.getMessage ()
							.clearValue ( "_key" )
							.clearValue ( "_item" )
						;
					}
					else
					{
						// skip it
					}
					return true;
				}
			} );
		}
	}

	private final String fSet;
	private final String fPipelineRef;
	private final Pipeline fPipeline;
	private final String fVarName;

	private void runProcessing ( MessageProcessingContext context )
	{
		if ( fPipeline != null )
		{
			if ( fPipeline.size () == 0 )
			{
				context.warn ( "Pipeline is empty." );
				return;
			}
			fPipeline.process ( context );
		}
		else
		{
			final String pipelineName = context.evalExpression ( fPipelineRef );
			if ( pipelineName.isEmpty () )
			{
				context.warn ( "Empty pipeline reference after evaluating " + fPipelineRef + "." );
				return;
			}

			final Pipeline pl = context.getStreamProcessingContext ().getProgram ().getPipeline ( pipelineName );
			if ( pl == null )
			{
				context.warn ( "Unknown pipeline " + pipelineName + " (" + fPipelineRef + ")." );
				return;
			}
			pl.process ( context );
		}
	}
}
