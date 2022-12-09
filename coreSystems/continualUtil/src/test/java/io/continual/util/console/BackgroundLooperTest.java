package io.continual.util.console;

import org.junit.Test;

import junit.framework.TestCase;

public class BackgroundLooperTest extends TestCase
{
	@Test
	public void testLoop ()
	{
		BackgroundLooper bl = new BackgroundLooper ( 1000 );
		assertFalse( bl.loop( null ) );
	}

	@Test
	public void testSetup ()
	{
		BackgroundLooper bl = new BackgroundLooper ( 1000 );
		bl.setup( null , null );
		bl.teardown( null );
		assertNotNull( bl );
	}
}
