package io.continual.util.data.base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

public class Base64InputStreamTest {
	
	@Test
    public void read() throws IOException{
//		dGVzdA==    -->  Base64( "test" )
		InputStream inputStream = new ByteArrayInputStream("dGVzdA==".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		int firstLetter = stream.read();
		Assert.assertEquals('t', firstLetter);
		Assert.assertEquals(2, stream.available());
		
		int secondLetter = stream.read();
		Assert.assertEquals('e', secondLetter);
		Assert.assertEquals(1, stream.available());
		
		int thirdLetter = stream.read();
		Assert.assertEquals('s', thirdLetter);
		Assert.assertEquals(1, stream.available());
		
		int fourthLetter = stream.read();
		Assert.assertEquals('t', fourthLetter);
		Assert.assertEquals(0, stream.available());
		
		stream.close();
    }
	
	@Test(expected = IOException.class)
    public void read_short_base64() throws IOException{
		InputStream inputStream = new ByteArrayInputStream("dGU".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		stream.read();
		stream.close();
    }
	
	@Test
    public void available_start_with_newline() throws IOException{
		InputStream inputStream = new ByteArrayInputStream("\ndGVzdA".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		Assert.assertEquals(3, stream.available());
		stream.close();
    }
	
	@Test
    public void available_start_with_only_newline() throws IOException{
		InputStream inputStream = new ByteArrayInputStream("\n".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		Assert.assertEquals(0, stream.available());
		stream.close();
    }
	
	@Test
    public void read_start_with_only_newline() throws IOException{
		InputStream inputStream = new ByteArrayInputStream("\n".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		Assert.assertEquals(-1, stream.read());
		stream.close();
    }
	
	@Test
    public void read_equals_sign() throws IOException{
		InputStream inputStream = new ByteArrayInputStream("NDU=".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		Assert.assertEquals(52, stream.read());
		stream.close();
    }	
	
	@Test
    public void read_plain_text() throws IOException{
		InputStream inputStream = new ByteArrayInputStream("te54erf==".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		int firstLetter = stream.read();
		Assert.assertEquals(181, firstLetter);
		Assert.assertEquals(2, stream.available());
		stream.close();
    }
		
	@Test(expected = IllegalArgumentException.class)
    public void read_wrongFormat1() throws IOException{
		InputStream inputStream = new ByteArrayInputStream("d.GU".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		stream.read();
		stream.close();
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void read_wrongFormat2() throws IOException{
		InputStream inputStream = new ByteArrayInputStream(".dGU".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		stream.read();
		stream.close();
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void read_wrongFormat3() throws IOException{
		InputStream inputStream = new ByteArrayInputStream("dh.U".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		stream.read();
		stream.close();
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void read_wrongFormat4() throws IOException{
		InputStream inputStream = new ByteArrayInputStream("dlG.".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		stream.read();
		stream.close();
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void read_wrongFormat5() throws IOException{
		InputStream inputStream = new ByteArrayInputStream("dlGü".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		stream.read();
		stream.close();
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void read_wrongFormat6() throws IOException{
		InputStream inputStream = new ByteArrayInputStream("dlük".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		stream.read();
		stream.close();
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void read_wrongFormat7() throws IOException{
		InputStream inputStream = new ByteArrayInputStream("düGk".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		stream.read();
		stream.close();
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void read_wrongFormat8() throws IOException{
		InputStream inputStream = new ByteArrayInputStream("ülGk".getBytes());
		Base64InputStream stream = new Base64InputStream(inputStream);
		stream.read();
		stream.close();
    }
	
	
}
