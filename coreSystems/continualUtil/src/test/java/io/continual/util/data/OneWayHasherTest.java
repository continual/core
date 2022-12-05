package io.continual.util.data;

import org.junit.Assert;
import org.junit.Test;

import io.continual.util.data.TypeConvertor.conversionError;

public class OneWayHasherTest {
	
	@Test
    public void digest(){
		String expected = "704cd272ac64db1e13f368ea1211b92b4b8038cf";
		Assert.assertEquals(expected, OneWayHasher.digest("test"));
    }
	
	@Test
    public void hash(){
		String expected = "ead1435fcc4b9c07e64ed61601286f9082927bf2";
		Assert.assertEquals(expected, OneWayHasher.hash("test", "-"));
    }
	
	@Test
    public void pbkdf2HashToString(){
		String expected = "ead1435fcc4b9c07e64ed61601286f9082927bf2";
		Assert.assertEquals(expected, OneWayHasher.pbkdf2HashToString("test", "-"));
    }
	
	@Test
    public void pbkdf2Hash() throws conversionError{
		byte[] expected = TypeConvertor.hexToBytes("ead1435fcc4b9c07e64ed61601286f9082927bf2");
		Assert.assertArrayEquals(expected, OneWayHasher.pbkdf2Hash("test", "-"));
    }
	
}
