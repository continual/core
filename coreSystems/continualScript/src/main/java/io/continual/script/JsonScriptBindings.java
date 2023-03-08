package io.continual.script;

import org.json.JSONObject;

import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.JsonEval;

public class JsonScriptBindings implements ScriptBindings<Object>
{
	public JsonScriptBindings ()
	{
		this ( new JSONObject () );
	}

	public JsonScriptBindings ( JSONObject obj )
	{
		fData = obj;
	}

	@Override
	public Object get ( String id )
	{
		final Object obj = JsonEval.eval ( fData, id );
		return obj == null ? null : obj.toString ();
	}

	public String getAsString ( String id )
	{
		final Object val = get ( id );
		return val == null ? null : val.toString ();
	}
	
	public boolean getAsBoolean ( String id )
	{
		final Object val = get ( id );
		return val == null ? null : TypeConvertor.convertToBooleanBroad ( val.toString () );
	}

	@Override
	public void set ( String id, Object val )
	{
		JsonEval.setValue ( fData, id, val );
	}

	private final JSONObject fData;
}
