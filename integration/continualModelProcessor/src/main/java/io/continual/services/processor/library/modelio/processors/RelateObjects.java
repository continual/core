package io.continual.services.processor.library.modelio.processors;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;

public class RelateObjects implements Processor
{
	public RelateObjects ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
	}
	
	@Override
	public void process ( MessageProcessingContext context )
	{
	}

}
