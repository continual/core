package io.continual.util.data.exprEval;

import org.junit.Test;

import io.continual.util.time.Clock;
import junit.framework.TestCase;

public class SpecialFnsDataSourceTest extends TestCase
{
	@Test
	public void testEval ()
	{
		final SpecialFnsDataSource sfds = new SpecialFnsDataSource ();
		assertTrue ( Clock.now () <= (long)sfds.eval ( "now" ) );
		assertTrue ( Clock.now () <= (long)sfds.eval ( "nowMs" ) );
		assertTrue ( ( Clock.now () / 1000L ) <= (long)sfds.eval ( "nowSec" ) );
		assertNotNull ( sfds.eval ( "uuid" ) );
		assertNull ( sfds.eval ( "key" ) );
	}
}
