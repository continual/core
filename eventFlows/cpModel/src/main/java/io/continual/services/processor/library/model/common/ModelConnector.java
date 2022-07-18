package io.continual.services.processor.library.model.common;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.builder.sources.BuilderJsonDataSource;
import io.continual.services.model.core.Model;
import io.continual.services.processor.config.readers.ConfigLoadContext;

public class ModelConnector
{
	public static Model modelFromConfig ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		return Builder.withBaseClass ( Model.class )
			.withClassNameInData ()
			.usingData ( new BuilderJsonDataSource ( config ) )
			.providingContext ( sc.getServiceContainer () )
			.build ();
	}
	
	public ModelConnector ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( modelFromConfig ( sc, config ) );
	}

	public ModelConnector ( Model model ) throws BuildFailure
	{
		try
		{
			fModel = model;
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	public Model getModel () { return fModel; }
	
	private final Model fModel;
}
