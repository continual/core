package io.continual.services;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.continual.services.Service.FailedToStart;
import junit.framework.TestCase;

public class SimpleServiceTest extends TestCase
{
	@Test
	public void testStartFinish ()
	{
		final SimpleService ss = new SimpleService ();
		try {
			ss.start ();
			assertTrue ( ss.isRunning () );
			ss.requestFinish ();
			assertFalse ( ss.isRunning () );
		} catch (FailedToStart e) {
			fail ( "Expected to execute but failed. Exception - " + e.getMessage () );
		}
	}

	@Test
	public void testRequestFinishAndWait ()
	{
		final SimpleService ss = new SimpleService ( null , null );
		try {
			ss.start ();
			assertTrue ( ss.isRunning () );
			ss.requestFinishAndWait ( 1 , TimeUnit.MILLISECONDS );
			assertFalse ( ss.isRunning () );
		} catch (FailedToStart | InterruptedException e) {
			fail ( "Expected to execute but failed. Exception - " + e.getMessage () );
		}
	}
}
