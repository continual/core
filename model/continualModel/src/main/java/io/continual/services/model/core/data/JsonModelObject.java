package io.continual.services.model.core.data;

import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import io.continual.util.data.json.JsonUtil;

/**
 * A generic in-memory object data structure
 */
public class JsonModelObject implements ModelObject
{
	public JsonModelObject ()
	{
		fData = new JSONObject ();
	}

	public JsonModelObject ( JSONObject data )
	{
		fData = data;
	}

	/**
	 * Clear the data in this object
	 * @return this object
	 */
	public JsonModelObject clear ()
	{
		fData.clear ();
		return this;
	}

	public JsonModelObject merge ( ModelObject data )
	{
		JsonUtil.overlay ( fData, modelObjectToJson ( data ) );
		return this;
	}

	/**
	 * Get the keys in this object.
	 * @return a set of keys
	 */
	@Override
	public Set<String> getKeys ()
	{
		return fData.keySet ();
	}

	/**
	 * Get a value generically. ModelDataNullValue.NULL indicates an explicit
	 * null value. Java null indicates the key is not present.
	 * @param key
	 * @return a value, or null if the key is not present
	 */
	@Override
	public Object get ( String key )
	{
		final Object val = fData.opt ( key );
		return val == null ? null : jsonToIface ( val );
	}

	/**
	 * Put a null value into the map
	 * @param key
	 * @return this writer
	 */
	@Override
	public JsonModelObject putNull ( String key )
	{
		fData.put ( key, JSONObject.NULL );
		return this;
	}

	/**
	 * Put a string into this map
	 * @param key
	 * @param val
	 * @return this writer
	 */
	@Override
	public JsonModelObject put ( String key, String val )
	{
		fData.put ( key, val );
		return this;
	}

	/**
	 * Put a number into this map
	 * @param key
	 * @param val
	 * @return this writer
	 */
	@Override
	public JsonModelObject put ( String key, Number val )
	{
		fData.put ( key, val );
		return this;
	}

	/**
	 * Put a boolean into this map
	 * @param key
	 * @param val
	 * @return this writer
	 */
	@Override
	public JsonModelObject put ( String key, boolean val )
	{
		fData.put ( key, val );
		return this;
	}

	/**
	 * Put a map into this map
	 * @param key
	 * @param val
	 * @return this writer
	 */
	@Override
	public JsonModelObject put ( String key, ModelObject val )
	{
		fData.put ( key, ifaceToJson ( val ) );
		return this;
	}

	/**
	 * Put a list into this map
	 * @param key
	 * @param val
	 * @return this writer
	 */
	@Override
	public JsonModelObject put ( String key, ModelList val )
	{
		fData.put ( key, ifaceToJson ( val ) );
		return this;
	}

	/**
	 * Utility method to translate from a model object to the equivalent JSON structure
	 * @param mo
	 * @return a JSON object
	 */
	public static JSONObject modelObjectToJson ( ModelObject mo )
	{
		// shortcut...
		if ( mo instanceof JsonModelObject )
		{
			return JsonUtil.clone ( ((JsonModelObject)mo).fData );
		}

		// general purpose...
		return (JSONObject) ifaceToJson ( mo );
	}
	
	private final JSONObject fData;

	// internal JSON to interface
	static Object jsonToIface ( Object internalObject )
	{
		if ( internalObject instanceof JSONObject )
		{
			return new JsonModelObject ( (JSONObject) internalObject );
		}
		else if ( internalObject instanceof JSONArray )
		{
			return new JsonModelList ( (JSONArray) internalObject );
		}
		else if ( internalObject == JSONObject.NULL )
		{
			return ModelObjectNullValue.NULL;
		}
		else
		{
			return internalObject;
		}
	}

	// interface object to JSON
	static Object ifaceToJson ( Object ifaceObject )
	{
		// in anticipation of making this an interface, let's not assume we can just use fData
		if ( ifaceObject instanceof ModelObject )
		{
			final ModelObject mo = (ModelObject) ifaceObject;
			final JSONObject result = new JSONObject ();
			for ( Map.Entry<String,Object> e :  mo.entrySet () )
			{
				result.put ( e.getKey (), ifaceToJson ( e.getValue () ) );
			}
			return result;
		}
		else if ( ifaceObject instanceof ModelList )
		{
			final ModelList ml = (ModelList) ifaceObject;
			final JSONArray result = new JSONArray ();
			for ( int i=0; i<ml.size (); i++ )
			{
				result.put ( ifaceToJson ( ml.get ( i ) ) );
			}
			return result;
		}
		else if ( ifaceObject == ModelObjectNullValue.NULL )
		{
			return JSONObject.NULL;
		}
		else
		{
			return ifaceObject;
		}
	}
}
