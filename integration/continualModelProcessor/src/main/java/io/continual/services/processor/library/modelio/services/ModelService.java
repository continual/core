package io.continual.services.processor.library.modelio.services;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.services.model.core.Model;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.service.SimpleProcessingService;

public class ModelService extends SimpleProcessingService
{
	public ModelService ( ConfigLoadContext clc, JSONObject config ) throws BuildFailure
	{
		try
		{
//			final ExpressionEvaluator ee = clc.getServiceContainer ().getExprEval ( config );

			fModel = Builder.withBaseClass ( Model.class )
				.providingContext ( clc.getServiceContainer () )
				.usingData ( config.getJSONObject ( "model" ) )
				.withClassNameInData ()
				.build ()
			;
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	public Model getModel ()
	{
		return fModel;
	}

	private final Model fModel;
}
