package io.continual.util.data.exprEval;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.TestCase;

public class ExprDataSourceStackTest extends TestCase
{
	private final String label = "key";

	@Test
	public void testEval1 ()
	{
		final ExprDataSourceStack edss = new ExprDataSourceStack ();
		assertNull ( edss.eval ( label ) );
	}

	@Test
	public void testEval2 ()
	{
		final Map<String, String> keyVal = new HashMap<> ();
		keyVal.put ( label , label );
		final MapDataSource[] arrmds = new MapDataSource[] {
			new MapDataSource ( new HashMap<> () ) , new MapDataSource ( keyVal )	
		};
		final ExprDataSourceStack edss = new ExprDataSourceStack ( arrmds );
		assertEquals ( label , edss.eval ( label ) );
	}
}
