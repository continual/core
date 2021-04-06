package io.continual.services.processor.library.influxdb.common;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.util.data.exprEval.ExpressionEvaluator;

public class IdbConnector
{
	public static IdbConnection dbConnectionFromConfig ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		final ExpressionEvaluator ee = sc != null ? sc.getServiceContainer ().getExprEval ( config ) : new ExpressionEvaluator ();
		return new IdbConnection ( config, ee );
	}
	
	public IdbConnector ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ( dbConnectionFromConfig ( sc, config ), sc, config );
	}

	public IdbConnector ( IdbConnection dbsrc, ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		try
		{
			fDb = dbsrc;
		}
		catch ( JSONException e )
		{
			throw new BuildFailure ( e );
		}
	}

	public IdbConnection getDb () { return fDb; }

	private final IdbConnection fDb;
}
