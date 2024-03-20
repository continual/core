package io.continual.util.data.csv;

import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CsvLineBuilderTest {
	
	private CsvLineBuilder builder;
	
	@Before
	public void init() {
		builder = new CsvLineBuilder();
	}
	
	@Test
    public void appendEmpty(){
		builder.appendEmpty();
		Assert.assertEquals("", builder.toString());
    }
	
	@Test
    public void append_boolean(){
		builder.append(true);
		Assert.assertEquals("true", builder.toString());
    }
	
	@Test
    public void append_date(){
		builder.append(new Date(1000000000000L));
		Assert.assertEquals("09/09/01 01:46:40", builder.toString());
    }
	
	@Test
    public void append_double(){
		builder.append(1.4d);
		Assert.assertEquals("1.4", builder.toString());
    }
	
	@Test
    public void append_long(){
		builder.append(555L);
		Assert.assertEquals("555", builder.toString());
    }
	
	@Test
    public void append_string(){
		builder.append("this is test");
		Assert.assertEquals("\"this is test\"", builder.toString());
    }
	
	@Test
    public void append_literal(){
		builder.appendLiteral("UT");
		Assert.assertEquals("UT", builder.toString());
    }
	
	@Test
    public void append_literal2(){
		CsvLineBuilder builder = new CsvLineBuilder('*','-',true);
		builder.appendLiteral("UT");
		builder.appendLiteral("GW");
		Assert.assertEquals("UT-GW", builder.toString());
    }
}
