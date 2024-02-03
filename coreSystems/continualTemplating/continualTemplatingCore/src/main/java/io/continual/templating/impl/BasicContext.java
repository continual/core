package io.continual.templating.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.continual.templating.ContinualTemplateContext;

public class BasicContext implements ContinualTemplateContext
{
	public BasicContext ()
	{
		fMap = new HashMap<>();
	}

	@Override
	public Object get ( String key )
	{
		return fMap.get ( key );
	}

	@Override
	public Set<String> keys ()
	{
		return Collections.unmodifiableSet ( fMap.keySet () );
	}

	@Override
	public ContinualTemplateContext put ( String key, Object o )
	{
		if ( key == null ) throw new NullPointerException ( "Cannot put a null key." );
		if ( o == null ) throw new NullPointerException ( "Cannot put a null value." );
		fMap.put ( key, o );
		return this;
	}

	@Override
	public ContinualTemplateContext remove ( String key )
	{
		if ( key == null ) throw new NullPointerException ( "Cannot remove a null key." );
		fMap.remove ( key );
		return this;
	}

	public Map<String,Object> getAsMap () { return fMap; }

	private final HashMap<String,Object> fMap;
}
