package io.continual.util.data.csv;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Vector;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CsvStreamTest {
	
	private CsvStream stream;
	private CsvStream streamWithHeaders;
	
	@Before
	public void init() throws IOException {
		stream = new CsvStream(false);
		InputStream targetStream = new ByteArrayInputStream("this is test".getBytes());
		stream.read(targetStream);
		Assert.assertEquals(1, stream.getRecordCount());
		Assert.assertNotNull(stream.getRecord(0));
		Assert.assertNotNull(stream.getRecord(0).get("0"));
		Assert.assertEquals("this is test", stream.getRecord(0).get("0"));
		
		streamWithHeaders = new CsvStream(true);
		BufferedReader targetStream2 = new BufferedReader(new StringReader("headerLine \r\n second line"));
		streamWithHeaders.read(targetStream2);
		Assert.assertEquals(1, streamWithHeaders.getRecordCount());
		Assert.assertNotNull(streamWithHeaders.getRecord(0));
		Assert.assertNotNull(streamWithHeaders.getRecord(0).get("headerLine"));
		Assert.assertEquals("second line", streamWithHeaders.getRecord(0).get("headerLine"));
	}
	
	@Test
    public void z_reset() throws IOException{
		stream.reset();
		Assert.assertEquals(0, stream.getRecordCount());
		Assert.assertEquals(0, stream.getLinesParsed());
    }
	
	@Test
    public void getColumnNames() throws IOException{		
		Vector<String> cols = streamWithHeaders.getColumnNames();
		Assert.assertNotNull(cols);
		Assert.assertEquals(1, cols.size());
		Assert.assertEquals("headerLine", cols.get(0));
    }
	
	@Test
    public void getField(){	
		Assert.assertEquals("this is test", stream.getField(0, 0));
		Assert.assertEquals("second line", streamWithHeaders.getField(0, 0));
    }
	
	@Test
    public void getField2(){	
		Assert.assertEquals(null, streamWithHeaders.getField(0, 6));
    }
	
	@Test
    public void parseHeaderLine(){
		List<String> headers = CsvStream.parseHeaderLine("test", '*', '-');
		Assert.assertNotNull(headers);
		Assert.assertEquals(1, headers.size());
		Assert.assertEquals("test", headers.get(0));
    }
	
	@Test
    public void parseHeaderLine2(){
		List<String> headers = CsvStream.parseHeaderLine("te*-", '*', '-');
		Assert.assertNotNull(headers);
		Assert.assertEquals(1, headers.size());
		Assert.assertEquals("te*", headers.get(0));
    }
	
	@Test
    public void parseHeaderLine3(){
		List<String> headers = CsvStream.parseHeaderLine("-te*-", '*', '-');
		Assert.assertNotNull(headers);
		Assert.assertEquals(2, headers.size());
		Assert.assertEquals("unnamed-1", headers.get(0));
		Assert.assertEquals("te*", headers.get(1));
    }

	@Test
	public void read() throws IOException {
		CsvStream stream = new CsvStream(false);
		InputStream targetStream = new ByteArrayInputStream("this,test".getBytes());
		stream.read(targetStream);
		Assert.assertEquals(1, stream.getRecordCount());
		Assert.assertNotNull(stream.getRecord(0));
		Assert.assertNotNull(stream.getRecord(0).get("0"));
		Assert.assertEquals("this", stream.getRecord(0).get("0"));
	}

	@Test
	public void read2() throws IOException {
		CsvStream stream = new CsvStream(true);
		InputStream targetStream = new ByteArrayInputStream("HEADER \r\n 1st line \r\n 2nd line,lines".getBytes());
		stream.read(targetStream);
		Assert.assertEquals(2, stream.getRecordCount());
		Assert.assertNotNull(stream.getRecord(0));
		Assert.assertNotNull(stream.getRecord(0).get("HEADER"));
		Assert.assertEquals("1st line", stream.getRecord(0).get("HEADER"));
	}
	
	@Test
	public void read3() throws IOException {
		CsvStream stream = new CsvStream(true);
		BufferedReader targetStream = new BufferedReader(new StringReader("this,test"));
		stream.read(targetStream);
		Assert.assertEquals(0, stream.getRecordCount());
	}
	
	@Test
	public void read4() throws IOException {
		CsvStream stream = new CsvStream(true);
		BufferedReader targetStream = new BufferedReader(new StringReader(",this,test"));
		stream.read(targetStream);
		Assert.assertEquals(3, stream.getColumnNames().size());
		Assert.assertEquals("unnamed-1", stream.getColumnNames().get(0));
		Assert.assertEquals("this", stream.getColumnNames().get(1));
		Assert.assertEquals("test", stream.getColumnNames().get(2));
	}
	
	
}
