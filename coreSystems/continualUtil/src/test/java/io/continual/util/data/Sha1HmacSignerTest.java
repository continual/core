package io.continual.util.data;

import org.junit.Assert;
import org.junit.Test;

public class Sha1HmacSignerTest {
	
	@Test
    public void sign(){
		Assert.assertEquals("T0NMPpkfG2fY2hW2jmaR31OIsuE=", Sha1HmacSigner.sign("test", "kkk"));
    }
	
}
