package io.continual.util.data;

import org.junit.Assert;
import org.junit.Test;

public class Sha256HmacSignerTest {
	
	@Test
    public void sign(){
		Assert.assertEquals("kpLBL9t9Mk1UC1LvryM4vCEzAjbwIFRED5iziOiR8rI=", Sha256HmacSigner.sign("test", "kkk"));
    }
	
	@Test
    public void signToBytes(){
		byte[] expected = TypeConvertor.base64Decode("kpLBL9t9Mk1UC1LvryM4vCEzAjbwIFRED5iziOiR8rI=");
		Assert.assertArrayEquals(expected, Sha256HmacSigner.signToBytes("test", "kkk"));
    }
	
}
