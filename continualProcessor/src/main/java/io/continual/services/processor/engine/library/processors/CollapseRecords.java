package io.continual.services.processor.engine.library.processors;

import java.util.ArrayList;

import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;

public class CollapseRecords implements Processor
{
	public CollapseRecords ()
	{
		fKeys = null;
		fLastRecord = 0;
	}
	
	public CollapseRecords onKey ( String... keyFields )
	{
		fKeys = keyFields;
		return this;
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		if ( fKeys == null ) return;

		final ArrayList<String> vals = new ArrayList<> ();
		for ( String key : fKeys )
		{
			vals.add ( context.getMessage ().getValueAsString ( key ) );
		}

		int valHash = vals.hashCode ();
		if ( valHash == fLastRecord )
		{
			context.stopProcessing ();
		}

		fLastRecord = valHash;
	}

	private String[] fKeys;
	private int fLastRecord;
}
