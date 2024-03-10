package io.continual.services.model.core.data;

import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import io.continual.util.data.json.JsonUtil;

public class JsonObjectAccess implements ModelDataObjectWriter
{
	public JsonObjectAccess ( JSONObject data )
	{
		fData = data;
	}

	@Override
	public Set<String> getKeys ()
	{
		return fData.keySet ();
	}

	@Override
	public Object get ( String key )
	{
		return wrap ( fData.opt ( key ) );
	}

	@Override
	public void clear ()
	{
		fData.clear ();
	}

	@Override
	public ModelDataObjectWriter merge ( ModelDataObjectAccess data )
	{
		JsonUtil.overlay ( fData, ModelDataToJson.translate ( data ) );
		return this;
	}

	@Override
	public String toString ()
	{
		return fData.toString ();
	}

	static Object wrap ( Object jsonVal )
	{
		if ( jsonVal instanceof JSONObject )
		{
			return new JsonObjectAccess ( (JSONObject) jsonVal );
		}
		else if ( jsonVal instanceof JSONArray )
		{
			return new JsonArrayAccess ( (JSONArray) jsonVal );
		}
		else if ( jsonVal == JSONObject.NULL )
		{
			return ModelDataNullValue.NULL;
		}
		else
		{
			return jsonVal;
		}
	}

	private final JSONObject fData;
}
