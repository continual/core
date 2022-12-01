package io.continual.util.time;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
		oldValueTimeStart = System.getProperty( "timeStart" );
		oldValueTimeScale = System.getProperty( "timeScale" );
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
		System.clearProperty( "timeStart" );
		System.clearProperty( "timeScale" );
		assertNotNull( Clock.now() );

		System.setProperty( "timeStart" , ""+timeStart );
		assertNotNull( Clock.now() );

		System.setProperty( "timeScale" , ""+timeScale );
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
		System.clearProperty( "timeStart" );
		System.clearProperty( "timeScale" );
		final Clock.ScaledClock sc = new Clock.ScaledClock();
		assertTrue( System.currentTimeMillis() <= sc.nowMs() );
	}

	@Test
	public void testScaledClockWithProps ()
	{
		System.setProperty( "timeStart" , ""+timeStart );
		System.setProperty( "timeScale" , ""+timeScale );
		final Clock.ScaledClock sc = new Clock.ScaledClock();
		assertTrue( System.currentTimeMillis() <= sc.nowMs() );
	}

	@Test
	public void testScaledClockExceptionProps ()
	{
		System.setProperty( "timeStart" , "" );
		System.setProperty( "timeScale" , "" );
		final Clock.ScaledClock sc = new Clock.ScaledClock();
		assertTrue( System.currentTimeMillis() <= sc.nowMs() );		
	}

	@Test
	public void testScaledClockInvalidPosStartNegScale ()
	{
		System.setProperty( "timeStart" , ""+(-timeStart) );
		System.setProperty( "timeScale" , ""+(-timeScale) );
		final Clock.ScaledClock sc = new Clock.ScaledClock();
		assertTrue( System.currentTimeMillis() <= sc.nowMs() );		
	}

	@After
	protected void tearDown () throws Exception
	{
		if( oldValueTimeStart != null )
		{
			System.setProperty( "timeStart" , oldValueTimeStart );
		}
		if( oldValueTimeScale != null )
		{
			System.setProperty( "timeScale" , oldValueTimeScale );
		}
	}
}
