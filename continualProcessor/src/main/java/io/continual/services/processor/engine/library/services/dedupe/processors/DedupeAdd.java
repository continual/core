package io.continual.services.processor.engine.library.services.dedupe.processors;

import org.json.JSONObject;

import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.library.services.dedupe.services.DedupeService;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;

public class DedupeAdd implements Processor
{
	public DedupeAdd ( ConfigLoadContext sc, JSONObject config )
	{
		fSvcName = config.getString ( "service" );
		fKey = config.getString ( "key" );
	}

	@Override
	public void process ( MessageProcessingContext ctx )
	{
		final DedupeService ds = ctx.getStreamProcessingContext().getNamedObject ( fSvcName, DedupeService.class );
		if ( ds == null )
		{
			ctx.warn ( "No dedupe service " + fSvcName + " found." );
			return;
		}
		ds.add ( ctx.evalExpression ( fKey ) );
	}

	private final String fSvcName;
	private final String fKey;
}
