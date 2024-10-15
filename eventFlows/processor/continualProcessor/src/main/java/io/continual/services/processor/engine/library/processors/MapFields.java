package io.continual.services.processor.engine.library.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.continual.services.processor.engine.model.MessageProcessingContext;
import io.continual.services.processor.engine.model.Processor;

public class MapFields implements Processor
{
	public MapFields ()
	{
		fKeys = null;
		fToField = null;
		fLookup = null;
		fMap = new HashMap<>();
	}
	
	public MapFields onKey ( String... keyFields )
	{
		fKeys = keyFields;
		return this;
	}

	public MapFields toField ( String field )
	{
		fToField = field;
		return this;
	}

	public interface ValueLookup
	{
		String lookup ( List<String> keyValues );
	}
	
	public MapFields usingLookup ( ValueLookup vl )
	{
		fLookup = vl;
		return this;
	}

	@Override
	public void process ( MessageProcessingContext context )
	{
		if ( fKeys == null || fToField == null || fLookup == null ) return;

		final ArrayList<String> vals = new ArrayList<> ();
		for ( String key : fKeys )
		{
			vals.add ( context.getMessage ().getString ( key ) );
		}

		int valHash = vals.hashCode ();
		String result = fMap.get ( valHash );
		if ( result == null )
		{
			result = fLookup.lookup ( vals );
			fMap.put ( valHash, result );
		}

		context.getMessage ().putValue ( fToField, result );
	}

	private String[] fKeys;
	private String fToField;
	private ValueLookup fLookup;
	private final HashMap<Integer,String> fMap;
}
