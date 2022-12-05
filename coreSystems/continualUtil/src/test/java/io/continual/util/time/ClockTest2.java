package io.continual.util.time;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import io.continual.util.time.Clock.TestClock;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClockTest2 {
	
	@Before
    public void setTheClock(){
		TestClock clock = Clock.useNewTestClock();
		clock.set(123l);
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
    public void sealedClockWrong(){

		Clock sealed = new Clock.ScaledClock();
		Assert.assertTrue(sealed.nowMs() > 1669751377425l);
    }
	
	@Test
    public void sealedClockWrong2(){
		System.setProperty("timeStart", "-1");
		System.setProperty("timeScale", "-1");
		Clock sealed = new Clock.ScaledClock();
		Assert.assertTrue(sealed.nowMs() > 1669751377425l);
    }
	
	@Test
    public void sealedClockWrong3(){
		System.setProperty("timeStart", "d");
		System.setProperty("timeScale", "e");
		Clock sealed = new Clock.ScaledClock();
		Assert.assertTrue(sealed.nowMs() > 1669751377425l);
    }
	
	@Test
    public void sealedClockWrong4(){
		System.setProperty("timeScale", "-1");
		Clock sealed = new Clock.ScaledClock();
		Assert.assertTrue(sealed.nowMs() > 1669751377425l);
    }
	
	@Test
    public void sealedClockZTrue(){
		System.setProperty("timeStart", "1");
		System.setProperty("timeScale", "1");
		Clock sealed = new Clock.ScaledClock();
		Assert.assertTrue(sealed.nowMs() > 1669751377425l);
    }
	
}
