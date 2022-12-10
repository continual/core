package io.continual.util.data.base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;

public class Base64OutputStreamTest {
	
	@Test
    public void write() throws IOException{
//		dGVzdA==    -->  Base64( "test" )
		
		OutputStream outputStream = new ByteArrayOutputStream();
		Base64OutputStream stream = new Base64OutputStream(outputStream);
		stream.write(116);
		stream.write(101);
		stream.write(115);
		stream.write(116);
		stream.close();
		Assert.assertEquals("dGVzdA==", outputStream.toString());
    }
	
	@Test
    public void write_long_text_for_buffer() throws IOException{		
		OutputStream outputStream = new ByteArrayOutputStream();
		Base64OutputStream stream = new Base64OutputStream(outputStream);
		for (int i = 32; i < 124; i++) {
			stream.write(i);
			stream.write(i);
		}
		stream.write(65);
		stream.write(65);
		stream.write(65);
		stream.write(65);
		stream.write(65);
		stream.write(65);
		stream.write(65);
		stream.write(65);
		stream.write(65);
		stream.close();
		String expectedBase64 = "ICAhISIiIyMkJCUlJiYnJygoKSkqKisrLCwtLS4uLy8wMDExMjIzMzQ0NTU2Njc3ODg5OTo6Ozs8PD09\n"
				+ "Pj4/P0BAQUFCQkNDRERFRUZGR0dISElJSkpLS0xMTU1OTk9PUFBRUVJSU1NUVFVVVlZXV1hYWVlaWltb\n"
				+ "XFxdXV5eX19gYGFhYmJjY2RkZWVmZmdnaGhpaWpqa2tsbG1tbm5vb3BwcXFycnNzdHR1dXZ2d3d4eHl5\n"
				+ "enp7e0FBQUFBQUFBQQ==";
		Assert.assertEquals(expectedBase64, outputStream.toString());
    }
	
	@Test
    public void write_max_per_line() throws IOException{		
		OutputStream outputStream = new ByteArrayOutputStream();
		Base64OutputStream stream = new Base64OutputStream(outputStream, 5);
		stream.write(116);
		stream.write(101);
		stream.write(115);
		stream.write(116);
		stream.close();
		Assert.assertEquals("dGVzd"
				+ "\n"
				+ "A==", outputStream.toString());
    }
	
	@Test
    public void write_max_per_line_negative() throws IOException{		
		OutputStream outputStream = new ByteArrayOutputStream();
		Base64OutputStream stream = new Base64OutputStream(outputStream, -100);
		stream.write(116);
		stream.write(101);
		stream.write(115);
		stream.write(116);
		stream.close();
		Assert.assertEquals("dGVzdA==", outputStream.toString());
    }
	
}
