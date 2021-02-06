package io.continual.util.data.exprEval;

import java.util.Map;

public class MapDataSource implements ExprDataSource
{
	public MapDataSource ( Map<String,?> map )
	{
		fMap = map;
	}

	@Override
	public Object eval ( String label )
	{
		return fMap.get ( label );
	}

	private final Map<String,?> fMap;
}
