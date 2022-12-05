package io.continual.util.data.csv;

import java.util.Date;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Test;

public class CsvEncoderTest {
	
	@Test
    public void encodeForCsv_basic(){
		Assert.assertEquals("test", CsvEncoder.encodeForCsv("test"));
    }
	
	@Test
    public void encodeForCsv_forceQuotes(){
		Assert.assertEquals("\"test\"", CsvEncoder.encodeForCsv("test", true));
    }
	
	@Test
    public void encodeForCsv_seperator(){
		Assert.assertEquals("*test*", CsvEncoder.encodeForCsv("test", '*', '-'));
    }
	
	@Test
    public void encodeForCsv(){
		Assert.assertEquals("", CsvEncoder.encodeForCsv(null, '*', '-', false));
    }
	
	@Test
    public void encodeForCsv2(){
		Assert.assertEquals("**", CsvEncoder.encodeForCsv("", '*', '-', false));
    }
	
	@Test
    public void encodeForCsv3(){
		Assert.assertEquals("*test\r*", CsvEncoder.encodeForCsv("test\r", '*', '-', false));
    }
	
	@Test
    public void encodeForCsv4(){
		Assert.assertEquals("*test\n*", CsvEncoder.encodeForCsv("test\n", '*', '-', false));
    }
	
	@Test
    public void encodeForCsv5(){
		Assert.assertEquals("*test**-*", CsvEncoder.encodeForCsv("test*-", '*', '-', false));
    }
	
	@Test
    public void encodeForCsv6(){
		Assert.assertEquals("*test-*", CsvEncoder.encodeForCsv("test-", '*', '-', false));
    }
	
	@Test
    public void encodeForCsv_date(){
		Assert.assertEquals("09/09/01 01:46:40", CsvEncoder.encodeForCsv(new Date(1000000000000L)));
    }
	
	@Test
    public void encodeForCsv_date_timezone(){
		Assert.assertEquals("2001-09-09T01:46Z", CsvEncoder.encodeForCsv(new Date(1000000000000L), TimeZone.getTimeZone ( "UTC" ), false));
    }
	
	@Test
    public void encodeForCsv_date_timezone_excel(){
		Assert.assertEquals("09/09/01 01:46:40", CsvEncoder.encodeForCsv(new Date(1000000000000L), TimeZone.getTimeZone ( "UTC" ), true));
    }
	
}
