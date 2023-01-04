package io.continual.util.data.exprEval;

import org.junit.Test;

import junit.framework.TestCase;

public class EnvDataSourceTest extends TestCase
{
	@Test
	public void testEval ()
	{
		assertEquals ( System.getenv ( "ds" ) , new EnvDataSource ().eval ( "ds" ) );
	}
}
