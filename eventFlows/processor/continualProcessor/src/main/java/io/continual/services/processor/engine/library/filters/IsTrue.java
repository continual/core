package io.continual.services.processor.engine.library.filters;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;

public class IsTrue extends Equals
{
    public IsTrue ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		super ( sc, config
			.put ( "left", config.getString ( "value" ) )
			.put ( "right", true )
		);
		fValue = config.getString ( "value" );
	}

	@Override
	public JSONObject toJson ()
	{
		final JSONObject result = new JSONObject ()
			.put ( "class", this.getClass ().getName () )
			.put ( "value", fValue )
		;
		return result;
	}

    private final String fValue;
}
