package io.continual.util.time;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.continual.util.time.Clock.TestClock;
import junit.framework.TestCase;

public class ClockTest extends TestCase 
{
	private final long timeMs = 1000;
	private final long timeStart = -604800000;	// 1 week ago
	private final long timeScale = 900;	// 15 minutes
	// System Properties
	private String oldValueTimeStart;
	private String oldValueTimeScale;

	@Before
	protected void setUp () throws Exception
	{
		oldValueTimeStart = System.getProperty ( Clock.skTimeStartMs );
		oldValueTimeScale = System.getProperty ( Clock.skTimeScaleArg );
	}

	@After
	protected void tearDown () throws Exception
	{
		// setup a normal clock
		Clock.replaceClock ( new Clock () );

		if ( oldValueTimeStart != null )
		{
			System.setProperty ( Clock.skTimeStartMs, oldValueTimeStart );
		}
		if ( oldValueTimeScale != null )
		{
			System.setProperty ( Clock.skTimeScaleArg, oldValueTimeScale );
		}
	}

	@Test
	public void testNow ()
	{
		final Clock clock = new Clock ();
		assertTrue( clock.nowMs() <= System.currentTimeMillis() );
		assertNotNull( Clock.now() );		
	}

	@Test
	public void testReplaceClock ()
	{
		final Clock clockObj = new Clock ();
		Clock.replaceClock(clockObj);
		assertTrue( Clock.now() <= System.currentTimeMillis() );
	}

	// Holder inner class
	@Test
	public void testHolder ()
	{
		System.clearProperty( Clock.skTimeStartMs );
		System.clearProperty( Clock.skTimeScaleArg );
		assertNotNull( Clock.now() );

		System.setProperty( Clock.skTimeStartMs , ""+timeStart );
		assertNotNull( Clock.now() );

		System.setProperty( Clock.skTimeScaleArg , ""+timeScale );
		assertNotNull( Clock.now() );
	}

	// TestClock inner class
	@Test
	public void testTestClockSet ()
	{
		final Clock.TestClock tc = new Clock.TestClock();
		tc.set( timeMs );
		assertEquals( timeMs , tc.nowMs() );
	}

	@Test
	public void testTestClockAdd ()
	{
		final Clock.TestClock tc = new Clock.TestClock();
		tc.add( timeMs );
		assertTrue( timeMs <= tc.nowMs() );
	}

	@Test
	public void testTestClockAddTU ()
	{
		final Clock.TestClock tc = new Clock.TestClock();
		tc.add( timeMs , TimeUnit.SECONDS );
		assertTrue( timeMs <= tc.nowMs() );
	}

	// ScaledClock inner class
	@Test
	public void testScaledClockWithNoProps ()
	{
		System.clearProperty ( Clock.skTimeStartMs );
		System.clearProperty ( Clock.skTimeScaleArg );

		final TestClock baseClock = Clock.useNewTestClock ();
		baseClock.set ( System.currentTimeMillis () );

		final Clock.ScaledClock sc = new Clock.ScaledClock ();
		assertEquals ( Clock.now (), sc.nowMs () );

		// move test clock time forward...
		baseClock.add ( 60 * 1000 );
		assertEquals ( Clock.now (), sc.nowMs () );
	}

	@Test
	public void testScaledClockWithProps ()
	{
		final TestClock baseClock = Clock.useNewTestClock ();

		System.setProperty ( Clock.skTimeStartMs, "" + timeStart );
		System.setProperty ( Clock.skTimeScaleArg, "" + timeScale );
		final Clock.ScaledClock sc = new Clock.ScaledClock ();

		assertEquals ( 900.0, sc.getScale () );
		assertEquals ( 1, sc.nowMs () );

		baseClock.add ( 1000 );
		assertEquals ( 900001, sc.nowMs () );
	}

	@Test
	public void testScaledClockExceptionProps ()
	{
		System.setProperty( Clock.skTimeStartMs , "" );
		System.setProperty( Clock.skTimeScaleArg , "" );
		final Clock.ScaledClock sc = new Clock.ScaledClock();
		assertTrue( System.currentTimeMillis() <= sc.nowMs() );		
	}

	@Test
	public void testScaledClockInvalidPosStartNegScale ()
	{
		System.setProperty ( Clock.skTimeStartMs, "" + ( -timeStart ) );
		System.setProperty ( Clock.skTimeScaleArg, "" + ( -timeScale ) );
		final Clock.ScaledClock sc = new Clock.ScaledClock ();
		assertEquals ( 1.0, sc.getScale () );
	}
}
