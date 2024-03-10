package io.continual.services.model.core.data;

import org.json.JSONArray;
import org.json.JSONObject;

public class ModelDataToJson
{
	public static JSONObject translate ( ModelDataObjectAccess mdoa )
	{
		final JSONObject result = new JSONObject ();
		for ( String key : mdoa.getKeys () )
		{
			result.put ( key, translate ( mdoa.get ( key ) ) );
		}
		return result;
	}

	public static JSONArray translate ( ModelDataListAccess mdaa )
	{
		final JSONArray result = new JSONArray ();
		for ( int i=0; i<mdaa.size (); i++ )
		{
			result.put ( translate ( mdaa.get ( i ) ) );
		}
		return result;
	}

	private static Object translate ( Object val )
	{
		if ( val instanceof ModelDataObjectAccess )
		{
			return translate ( (ModelDataObjectAccess) val );
		}
		else if ( val instanceof ModelDataListAccess )
		{
			return translate ( (ModelDataListAccess) val );
		}
		else
		{
			return val;
		}
	}
}
