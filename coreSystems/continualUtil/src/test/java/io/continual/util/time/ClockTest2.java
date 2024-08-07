package io.continual.util.time;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import io.continual.util.time.Clock.TestClock;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClockTest2 {
	
	@Before
	public void setTheClock ()
	{
		final TestClock clock = Clock.useNewTestClock ();
		clock.set ( 123l );
	}

	@After
	public void tearDown () throws Exception
	{
		// setup a normal clock
		Clock.replaceClock ( new Clock () );
	}

	@Test
    public void now(){
		Assert.assertEquals(123l, Clock.now());
    }
	
	@Test
    public void nowMs(){
		Assert.assertEquals(123l, Clock.now());
    }
	
	@Test
    public void nowInstanceTest(){
		Clock clock = new Clock();
		Assert.assertTrue(clock.nowMs() > 1669751377425l);
    }
	
	@Test
    public void set(){
		TestClock clock = Clock.useNewTestClock();
		clock.set(999l);
		Assert.assertEquals(999l, clock.nowMs());
    }
	
	@Test
    public void add(){
		TestClock clock = Clock.useNewTestClock();
		clock.set(1l);
		clock.add(43l);
		clock.add(11l);
		Assert.assertEquals(55l, clock.nowMs());
    }
	
	@Test
    public void addMili(){
		TestClock clock = Clock.useNewTestClock();
		clock.set(5l);
		clock.add(101l, TimeUnit.MILLISECONDS);
		clock.add(205l, TimeUnit.MILLISECONDS);
		Assert.assertEquals(311l, clock.nowMs());
    }
	
	@Test
    public void scaledClockWrong(){

		Clock scaled = new Clock.ScaledClock();
		Assert.assertTrue(scaled.nowMs() > 1669751377425l);
    }

	@Test
	public void scaledClockWrong2 ()
	{
		System.setProperty ( Clock.skTimeStartMs, "-1" );	// meaning start 1 ms before current time
		System.setProperty ( Clock.skTimeScaleArg, "-1" );	// resulting in a warning and use of 1.0

		final Clock.ScaledClock scaled = new Clock.ScaledClock ();
		Assert.assertEquals ( 122, scaled.nowMs () );
	}
	
	@Test
    public void scaledClockWrong3(){
		System.setProperty(Clock.skTimeStartMs, "d");
		System.setProperty(Clock.skTimeScaleArg, "e");
		Clock scaled = new Clock.ScaledClock();
		Assert.assertTrue(scaled.nowMs() > 1669751377425l);
    }
	
	@Test
    public void scaledClockWrong4(){
		System.setProperty(Clock.skTimeScaleArg, "-1");
		Clock scaled = new Clock.ScaledClock();
		Assert.assertTrue(scaled.nowMs() > 1669751377425l);
    }
	
	@Test
    public void scaledClockZTrue()
	{
		System.setProperty ( Clock.skTimeStartMs, "1" );
		System.setProperty ( Clock.skTimeScaleArg, "1" );

		final Clock scaled = new Clock.ScaledClock ();
		Assert.assertEquals ( 1, scaled.nowMs () );
    }
}
