package io.continual.util.data.exprEval;

import org.json.JSONObject;

import io.continual.util.data.json.JsonEval;

public class JsonDataSource implements ExprDataSource 
{
	public JsonDataSource ( JSONObject data )
	{
		fData = data;
	}

	@Override
	public Object eval ( String label )
	{
		return JsonEval.eval ( fData, label );
	}

	private final JSONObject fData;
}