package io.continual.services.model.core.data;

import org.json.JSONArray;

public class JsonArrayAccess implements ModelDataListAccess
{
	public JsonArrayAccess ( JSONArray data )
	{
		fData = data;
	}

	@Override
	public int size () { return fData.length (); }

	@Override
	public Object get ( int index )
	{
		return JsonObjectAccess.wrap ( fData.get ( index ) );
	}

	@Override
	public String toString ()
	{
		return fData.toString ();
	}
	
	private final JSONArray fData;
}
