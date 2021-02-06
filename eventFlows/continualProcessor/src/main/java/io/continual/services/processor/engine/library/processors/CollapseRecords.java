package io.continual.services.processor.engine.library.processors;

import java.util.ArrayList;
import java.util.LinkedList;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;
import io.continual.util.data.json.JsonVisitor;

public class CollapseRecords implements Processor
{
	public CollapseRecords ()
	{
		fKeys = new LinkedList<> ();
		fLastRecord = 0;
	}

	public CollapseRecords ( ConfigLoadContext sc, JSONObject config ) throws BuildFailure
	{
		this ();

		fKeys.addAll ( JsonVisitor.arrayToList ( config.optJSONArray ( "keys" ) ) );
	}

	public CollapseRecords onKey ( String... keyFields )
	{
		for ( String key : keyFields )
		{
			fKeys.add ( key );
		}
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

	private LinkedList<String> fKeys;
	private int fLastRecord;
}
