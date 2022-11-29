package io.continual.util.standarts;

import org.junit.Assert;
import org.junit.Test;

import io.continual.util.standards.HttpStatusCodes;


public class HttpStatusCodesTest {
	
	@Test
    public void isSuccess(){
		Assert.assertTrue(HttpStatusCodes.isSuccess(200));
    }
	
	@Test
    public void isSuccess2(){
		Assert.assertTrue(HttpStatusCodes.isSuccess(250));
    }
	
	@Test
    public void isSuccessNegative(){
		Assert.assertFalse(HttpStatusCodes.isSuccess(100));
    }
	
	@Test
    public void isSuccessNegative2(){
		Assert.assertFalse(HttpStatusCodes.isSuccess(300));
    }
	
	@Test
    public void isClientFailure(){
		Assert.assertTrue(HttpStatusCodes.isClientFailure(400));
    }
	
	@Test
    public void isClientFailureNegative(){
		Assert.assertFalse(HttpStatusCodes.isClientFailure(300));
    }
	
	@Test
    public void isClientFailureNegative2(){
		Assert.assertFalse(HttpStatusCodes.isClientFailure(500));
    }
	
	@Test
    public void isServerFailure(){
		Assert.assertTrue(HttpStatusCodes.isServerFailure(500));
    }
	
	@Test
    public void isServerFailureNegative(){
		Assert.assertFalse(HttpStatusCodes.isServerFailure(400));
    }
	
	@Test
    public void isFailure(){
		Assert.assertFalse(HttpStatusCodes.isFailure(200));
    }
	
	@Test
    public void isFailure2(){
		Assert.assertFalse(HttpStatusCodes.isFailure(100));
    }
	
	@Test
    public void isFailureNegative(){
		Assert.assertTrue(HttpStatusCodes.isFailure(400));
    }
	
	@Test
    public void isFailureNegative2(){
		Assert.assertTrue(HttpStatusCodes.isFailure(500));
    }

}
