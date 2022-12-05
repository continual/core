package io.continual.util.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Assert;
import org.junit.Test;

public class StreamToolsTest {
	
	@Test
    public void readBytes() throws IOException{
	    InputStream targetStream = new ByteArrayInputStream("test".getBytes());
		byte[] result = StreamTools.readBytes(targetStream);
		Assert.assertNotNull(result);
		Assert.assertArrayEquals("test".getBytes(), result);
    }
	
	@Test
    public void readBytes_buffer() throws IOException{
	    InputStream targetStream = new ByteArrayInputStream("test".getBytes());
		byte[] result = StreamTools.readBytes(targetStream, 200);
		Assert.assertNotNull(result);
		Assert.assertArrayEquals("test".getBytes(), result);
    }
	
	@Test
    public void readBytes_limit() throws IOException{
	    InputStream targetStream = new ByteArrayInputStream("test".getBytes());
		byte[] result = StreamTools.readBytes(targetStream, 200, 10);
		Assert.assertNotNull(result);
		Assert.assertArrayEquals("test".getBytes(), result);
    }

	@Test
    public void readBytes_limit2() throws IOException{
	    InputStream targetStream = new ByteArrayInputStream("test".getBytes());
		byte[] result = StreamTools.readBytes(targetStream, 200, -30);
		Assert.assertNotNull(result);
		Assert.assertArrayEquals("test".getBytes(), result);
    }
	
	@Test
    public void readBytes_limit3() throws IOException{
		byte[] result = StreamTools.readBytes(null, 200, -30);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.length);
    }
	
	@Test
    public void readBytes_limit4() throws IOException{
	    InputStream targetStream = new ByteArrayInputStream("test".getBytes());
		byte[] result = StreamTools.readBytes(targetStream, 200, 2);
		Assert.assertNotNull(result);
		Assert.assertArrayEquals("te".getBytes(), result);
    }
	
	@Test
    public void copyStream() throws IOException{
	    InputStream targetStream = new ByteArrayInputStream("test".getBytes());
	    OutputStream outStream = new ByteArrayOutputStream();
		StreamTools.copyStream(targetStream, outStream);
		Assert.assertNotNull(outStream);
		Assert.assertEquals("test", outStream.toString());
    }
	
	@Test
    public void copyStream_withSize() throws IOException{
	    InputStream targetStream = new ByteArrayInputStream("test".getBytes());
	    OutputStream outStream = new ByteArrayOutputStream();
		StreamTools.copyStream(targetStream, outStream, 10);
		Assert.assertNotNull(outStream);
		Assert.assertEquals("test", outStream.toString());
    }
	
	@Test
    public void copyStream_withSizeClose() throws IOException{
	    InputStream targetStream = new ByteArrayInputStream("test".getBytes());
	    OutputStream outStream = new ByteArrayOutputStream();
		StreamTools.copyStream(targetStream, outStream, 10, false);
		Assert.assertNotNull(outStream);
		Assert.assertEquals("test", outStream.toString());
    }
	
}
