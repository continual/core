package io.continual.services.processor.engine.library.filters;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;

public class IsFalse extends IsTrue
{
    public IsFalse ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config );
	}

	@Override
	public JSONObject toJson ()
	{
		return super.toJson ()
			.put ( "class", this.getClass ().getName () )
		;
	}
}
