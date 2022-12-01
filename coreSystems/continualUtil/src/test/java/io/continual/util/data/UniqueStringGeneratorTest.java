package io.continual.util.data;

import org.junit.Assert;
import org.junit.Test;

public class UniqueStringGeneratorTest {
	
	@Test
    public void create(){
		String result = UniqueStringGenerator.create("test");
		Assert.assertNotNull(result);
		Assert.assertEquals(40, result.length());
    }
	
	@Test
    public void create_null(){
		String result = UniqueStringGenerator.create(null);
		Assert.assertNotNull(result);
		Assert.assertEquals(40, result.length());
    }
	
	@Test
    public void createKeyUsingAlphabet(){
		String result = UniqueStringGenerator.createKeyUsingAlphabet("test", "abc");
		Assert.assertNotNull(result);
		Assert.assertEquals(20, result.length());
		for (int i = 0; i < result.length(); i++) {
			char letter = result.charAt(i);
			Assert.assertTrue(letter == 'a' || letter == 'b' || letter == 'c');
		}
    }
	
	@Test
    public void createKeyUsingAlphabet2(){
		String result = UniqueStringGenerator.createKeyUsingAlphabet("test", "abc", 100);
		Assert.assertNotNull(result);
		Assert.assertEquals(100, result.length());
		for (int i = 0; i < result.length(); i++) {
			char letter = result.charAt(i);
			Assert.assertTrue(letter == 'a' || letter == 'b' || letter == 'c');
		}
    }
	
	@Test
    public void createUrlKey(){
		String result = UniqueStringGenerator.createUrlKey("host");
		Assert.assertNotNull(result);
		Assert.assertEquals(20, result.length());
    }
	
	@Test
    public void createMsStyleKeyString(){
		// result format is JDWBY 59XR1 44DDQ 2G89F
		String result = UniqueStringGenerator.createMsStyleKeyString("host");
		Assert.assertNotNull(result);
		Assert.assertEquals(23, result.length());
		for (int i = 5; i < result.length(); i=i+6) {
			char letter = result.charAt(i);
			Assert.assertEquals(' ', letter);
		}
    }
	
	@Test
    public void createEncodedUuid(){
		String result = UniqueStringGenerator.createEncodedUuid();
		Assert.assertNotNull(result);
		Assert.assertEquals(22, result.length());
    }
	

	
}
