package io.continual.util.data.exprEval;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.TestCase;

public class MapDataSourceTest extends TestCase
{
	@Test
	public void testEval ()
	{
		final Map<String, String> keyVal = new HashMap<> ();
		keyVal.put ( "key" , "value" );
		final MapDataSource mds = new MapDataSource ( keyVal );
		assertNull ( mds.eval ( "name" ) );
		assertEquals ( "value" , mds.eval ( "key" ) );
	}
}
