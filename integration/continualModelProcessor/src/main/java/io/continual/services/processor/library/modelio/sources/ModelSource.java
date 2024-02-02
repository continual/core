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

package io.continual.services.processor.library.modelio.sources;

import java.io.IOException;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelObjectAndPath;
import io.continual.services.model.core.ModelObjectList;
import io.continual.services.model.core.ModelQuery;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.library.sources.BasicSource;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.engine.model.MessageAndRouting;
import io.continual.services.processor.engine.model.StreamProcessingContext;
import io.continual.services.processor.library.modelio.services.ModelService;
import io.continual.util.data.exprEval.ExpressionEvaluator;

public class ModelSource extends BasicSource
{
	public ModelSource ( ConfigLoadContext clc, JSONObject config ) throws BuildFailure
	{
		super ( config );
		try
		{
			final ExpressionEvaluator ee = clc.getServiceContainer ().getExprEval ( config );
			fModelSvcName = ee.evaluateText ( config.getString ( "modelName" ) );

			fResults = null;
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	private final String fModelSvcName;
	private ModelObjectList fResults;
	
	private static final Logger log = LoggerFactory.getLogger ( ModelSource.class );

	@Override
	protected synchronized MessageAndRouting internalGetNextMessage ( StreamProcessingContext spc ) throws IOException, InterruptedException
	{
		try
		{
			if ( fResults == null )
			{
				log.info ( "Executing query..." );

				final ModelService ms = spc.getNamedObject ( fModelSvcName, ModelService.class );
				if ( ms == null )
				{
					spc.fail ( "No model service named " + fModelSvcName + "." );
					fResults = ModelObjectList.emptyList ();	// prevent re-run
					return null;
				}

				final Model model = ms.getModel ();

				final ModelRequestContext mrc = model.getRequestContextBuilder ()
					.forUser ( spc.getOperator () )
					.build ()
				;
	
				final ModelQuery fQuery = model.startQuery();
				fResults = fQuery.execute ( mrc );
			}

			final Iterator<ModelObjectAndPath> iter = fResults.iterator ();
			if ( iter.hasNext () )
			{
				final ModelObjectAndPath mop = iter.next ();
				final JSONObject asJson = mop.getObject ().toJson ().getJSONObject ( "data" );
				asJson.put ( "modelPath", mop.getPath().toString () );
				final Message msg = Message.adoptJsonAsMessage ( asJson );
				return makeDefRoutingMessage ( msg );
			}
			else
			{
				noteEndOfStream ();
			}
		}
		catch ( BuildFailure | ModelRequestException | ModelServiceException e )
		{
			spc.fail ( e.getMessage () );
		}

		return null;
	}
}
