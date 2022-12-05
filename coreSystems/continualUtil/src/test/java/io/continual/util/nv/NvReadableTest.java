package io.continual.util.nv;

import org.junit.Assert;
import org.junit.Test;

public class NvReadableTest {
	
	@Test
    public void loadExceptionClass(){
		NvReadable.LoadException exception = new NvReadable.LoadException("system error");
		NvReadable.LoadException exception2 = new NvReadable.LoadException(new RuntimeException());
		Assert.assertNotNull(exception);
		Assert.assertNotNull(exception2);
	}
	
	@Test
    public void missingReqdSettingExceptionClass(){
		NvReadable.MissingReqdSettingException exception = new NvReadable.MissingReqdSettingException("key1");
		NvReadable.MissingReqdSettingException exception2 = new NvReadable.MissingReqdSettingException("key1", new RuntimeException());
		Assert.assertNotNull(exception);
		Assert.assertNotNull(exception2);
	}
	
	@Test
    public void invalidSettingValueExceptionClass(){
		NvReadable.InvalidSettingValueException exception = new NvReadable.InvalidSettingValueException("key1");
		NvReadable.InvalidSettingValueException exception2 = new NvReadable.InvalidSettingValueException("key1", new RuntimeException());
		NvReadable.InvalidSettingValueException exception3 = new NvReadable.InvalidSettingValueException("key1", "system error");
		NvReadable.InvalidSettingValueException exception4 = new NvReadable.InvalidSettingValueException("key1", new RuntimeException(), "system error");
		Assert.assertNotNull(exception);
		Assert.assertNotNull(exception2);
		Assert.assertNotNull(exception3);
		Assert.assertNotNull(exception4);
	}
	

}
