package io.continual.services.processor.engine.library.services.bucketing.processors;

import org.json.JSONObject;

import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.library.services.bucketing.BucketingService;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;

public class BucketAdd implements Processor
{
	public BucketAdd ( ConfigLoadContext sc, JSONObject config )
	{
		fSet = null;
		fSetNamed = config.getString ( "set" );
	}

	public BucketAdd ( BucketingService set )
	{
		fSet = set;
		fSetNamed = null;
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		BucketingService set = fSet;
		if ( set == null )
		{
			set = context.getStreamProcessingContext ().getNamedObject ( fSetNamed, BucketingService.class );
		}
		if ( set != null )
		{
			set.add ( context );
		}
		else if ( !fWarnedNotFound )
		{
			fWarnedNotFound = true;
			context.warn ( "Couldn't find bucket set." );
		}
	}

	private final BucketingService fSet;
	private final String fSetNamed;
	private boolean fWarnedNotFound = false;
}
