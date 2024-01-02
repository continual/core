package io.continual.services.model.core.filters;

import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelItemFilter;
import io.continual.util.data.json.JsonEval;

public class FieldValueEquals implements ModelItemFilter<ModelObject>
{
	public FieldValueEquals ( String field, Object val )
	{
		fField = field;
		fValue = val;
	}

	@Override
	public boolean matches ( ModelObject obj )
	{
		final Object val = JsonEval.eval ( obj.getData (), fField );

		if ( val == null && fValue == null ) return true;
		if ( val == null || fValue == null ) return false;

		return val.equals ( fValue );
	}

	private final String fField;
	private final Object fValue;
}
