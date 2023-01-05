package io.continual.util.data.exprEval;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.TestCase;

public class ExprDataSourceTest extends TestCase
{
	@Test
	public void testEvalToStringNoDefault ()
	{
		final TestExprDataSource teds = new TestExprDataSource ();
		assertNull ( teds.evalToString ( "key" ) );
	}

	@Test
	public void testEvalToStringWithDefault ()
	{
		final String expect = "value";
		final TestExprDataSource teds = new TestExprDataSource ();
		assertEquals ( expect , teds.evalToString ( "key" , expect ) );
		teds.keyVal.put ( "key" , expect );
		assertEquals ( expect , teds.evalToString ( "key" , expect ) );
	}

	private static class TestExprDataSource implements ExprDataSource
	{
		public Map <String , Object> keyVal = new HashMap<> ();

		@Override
		public Object eval(String label) {
			return keyVal.get ( label );
		}
	}
}
