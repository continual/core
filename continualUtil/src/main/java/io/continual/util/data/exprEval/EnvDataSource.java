package io.continual.util.data.exprEval;

public class EnvDataSource implements ExprDataSource
{
	@Override
	public Object eval ( String label )
	{
		return System.getenv ().get ( label );
	}
}
