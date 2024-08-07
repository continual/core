package io.continual.jsonHttpClient.impl.cache;

import org.junit.Test;

import io.continual.jsonHttpClient.TestResponse;
import junit.framework.TestCase;

public class NoopCacheTest extends TestCase
{
	@Test
	public void testCheckNoopCache ()
	{
		final NoopCache nc = new NoopCache ();
		nc.put ( "foo", new TestResponse ( 200, "ok" ) );
		assertNull ( nc.get ( "foo" ) );
	}
}
