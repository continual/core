package io.continual.services.model.core.filters;

import io.continual.services.model.core.ModelItemFilter;
import io.continual.services.model.core.data.ModelDataObjectAccess;
import io.continual.services.model.core.data.ModelDataObjectExprSource;
import io.continual.util.data.exprEval.ExpressionEvaluator;

public class FieldValueContainsText implements ModelItemFilter<ModelDataObjectAccess>
{
	public FieldValueContainsText ( String field, String text )
	{
		fField = field;
		fInnerText = text;
	}

	@Override
	public boolean matches ( ModelDataObjectAccess obj )
	{
		if ( fInnerText == null ) return false;

		final Object val = ExpressionEvaluator.evaluateSymbol ( fField, new ModelDataObjectExprSource ( obj ) );
		
		if ( val == null ) return false;

		return val.toString ().contains ( fInnerText );
	}

	private final String fField;
	private final String fInnerText;
}
