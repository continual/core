package io.continual.script;

import org.json.JSONObject;

import io.continual.util.data.json.JsonEval;

public class JsonScriptBindings implements ScriptBindings
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
	public String get ( String id )
	{
		final Object obj = JsonEval.eval ( fData, id );
		return obj == null ? null : obj.toString ();
	}

	@Override
	public void set ( String id, String val )
	{
		JsonEval.setValue ( fData, id, val );
	}

	private final JSONObject fData;
}
