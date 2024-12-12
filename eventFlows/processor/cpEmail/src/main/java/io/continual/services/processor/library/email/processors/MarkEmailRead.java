package io.continual.services.processor.library.email.processors;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;

public class MarkEmailRead implements Processor
{
	public MarkEmailRead ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
	}

}
