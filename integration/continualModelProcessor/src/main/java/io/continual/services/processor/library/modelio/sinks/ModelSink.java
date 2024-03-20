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

package io.continual.services.processor.library.modelio.sinks;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Sink;
import io.continual.services.processor.library.modelio.services.ModelService;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.naming.Path;

public class ModelSink implements Sink
{
	public ModelSink ( ConfigLoadContext clc, JSONObject config ) throws BuildFailure
	{
		try
		{
			final ExpressionEvaluator ee = clc.getServiceContainer ().getExprEval ( config );
			fModelSvcName = ee.evaluateText ( config.getString ( "modelName" ) );
			fModelPathExpr = config.getString ( "modelPath" );
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	@Override
	public synchronized void init ()
	{
	}

	@Override
	public synchronized void flush ()
	{
		// nothing to do here
	}

	@Override
	public synchronized void close ()
	{
		log.warn ( "ModelSink closing..." );
	}

	@Override
	public synchronized void process ( MessageProcessingContext context )
	{
		try
		{
			final ModelService ms = context.getStreamProcessingContext ().getNamedObject ( fModelSvcName, ModelService.class );
			if ( ms == null )
			{
				context.getStreamProcessingContext ().fail ( "No model service named " + fModelSvcName + "." );
				return;
			}

			final Model model = ms.getModel ();

			final ModelRequestContext mrc = model.getRequestContextBuilder ()
				.forUser ( context.getStreamProcessingContext ().getOperator () )
				.build ()
			;

			final Message msg = context.getMessage ();
			final String modelPathText = context.evalExpression ( fModelPathExpr );
			final Path modelPath = Path.fromString ( modelPathText ); 

			log.info ( "Writing to {}", modelPathText );

			model.createUpdate ( mrc, modelPath )
				.merge ( new JsonModelObject ( msg.toJson () ) )
				.execute ()
			;
		}
		catch ( BuildFailure | ModelRequestException | ModelServiceException e )
		{
			context.getStreamProcessingContext ().fail ( e.getMessage () );
		}
	}

	private final String fModelSvcName;
	private final String fModelPathExpr;

	private static final Logger log = LoggerFactory.getLogger ( ModelSink.class );
}
