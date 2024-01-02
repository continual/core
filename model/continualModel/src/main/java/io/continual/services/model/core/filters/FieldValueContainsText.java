package io.continual.services.model.core.filters;

import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelItemFilter;
import io.continual.util.data.json.JsonEval;

public class FieldValueContainsText implements ModelItemFilter<ModelObject>
{
	public FieldValueContainsText ( String field, String text )
	{
		fField = field;
		fInnerText = text;
	}

	@Override
	public boolean matches ( ModelObject obj )
	{
		if ( fInnerText == null ) return false;

		final Object val = JsonEval.eval ( obj.getData (), fField );
		if ( val == null ) return false;

		return val.toString ().contains ( fInnerText );
	}

	private final String fField;
	private final String fInnerText;
}
