package io.continual.util.data.csv;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Vector;

import org.junit.Assert;
import org.junit.Test;

import io.continual.util.data.csv.CsvInspector.fieldInfo;

public class CsvInspectorTest {
	
	@Test
	public void general() {		
		CsvInspector inspector = new CsvInspector();
		inspector.inputSample(Arrays.asList("first line", "second line", "third line"));		
		Assert.assertEquals(3, inspector.getRowCount());
		Assert.assertEquals("first line", inspector.getLine(0));
		Assert.assertEquals("second line", inspector.getLine(1));
		Assert.assertEquals("third line", inspector.getLine(2));
		Assert.assertEquals(1, inspector.getFieldCount());
		Vector<fieldInfo> lineInfo = inspector.getLineInfo(0);
		Assert.assertNotNull(lineInfo);
		Assert.assertEquals(1, lineInfo.size());
		Assert.assertNotNull(lineInfo.get(0));
		Assert.assertEquals(1, lineInfo.get(0).fTypeMask);
		Assert.assertEquals("first line", lineInfo.get(0).fValue);
		Assert.assertEquals("first line", lineInfo.get(0).value());
	}
	
	@Test
    public void readStreamForSample() {
		CsvInspector inspector = new CsvInspector();
		Assert.assertEquals(true, inspector.readStreamForSample(new ByteArrayInputStream("hey1 \r\n hey2 \r\n hey3".getBytes())));
    }
	
	@Test
    public void readStreamForSample2() {
		CsvInspector inspector = new CsvInspector();
		Assert.assertEquals(false, inspector.readStreamForSample(new ByteArrayInputStream("hey1".getBytes())));
    }

	@Test
    public void setHints() {
		CsvInspector inspector = new CsvInspector();
		inspector.setHints(true, '*', '-');
		Assert.assertEquals('*', inspector.getDelimiterChar());
		Assert.assertEquals('-', inspector.getQuoteChar());
		Assert.assertEquals(true, inspector.hasHeaderLine());
    }
	
	@Test
    public void detectType() {
		CsvInspector inspector = new CsvInspector();
		inspector.setHints(true, '*', '-');
		Assert.assertEquals('*', inspector.getDelimiterChar());
		Assert.assertEquals('-', inspector.getQuoteChar());
		Assert.assertEquals(true, inspector.hasHeaderLine());
    }
	
	@Test
	public void inputSample() {		
		CsvInspector inspector = new CsvInspector();
		inspector.inputSample(new String[] { "first line", "", "third line" } );		
		Assert.assertEquals(3, inspector.getRowCount());
		Assert.assertEquals("first line", inspector.getLine(0));
		Assert.assertEquals("", inspector.getLine(1));
		Assert.assertEquals("third line", inspector.getLine(2));
	}
	
	@Test
	public void inspectTypes() {
		CsvInspector inspector = new CsvInspector();
		Assert.assertTrue(inspector.inputSample(Arrays.asList( "123", "2022-12-31" , "hi,arzu", "special \" char", "t1" ) ));
		inspector.reevaluate();
		Assert.assertEquals(5, inspector.getRowCount());
		Assert.assertEquals(1, inspector.getTypePossibilities(0));
		
		Vector<fieldInfo> lineInfo = inspector.getLineInfo(0);
		Assert.assertEquals(CsvInspector.kTypeNumeric + 1, lineInfo.get(0).fTypeMask);
		Assert.assertEquals("123", lineInfo.get(0).fValue);
		
		Vector<fieldInfo> lineInfo2 = inspector.getLineInfo(1);
		Assert.assertEquals(CsvInspector.kTypeDate + 1, lineInfo2.get(0).fTypeMask);
		Assert.assertEquals("2022-12-31", lineInfo2.get(0).fValue);
		
		Vector<fieldInfo> lineInfo3 = inspector.getLineInfo(2);
		Assert.assertEquals(CsvInspector.kTypeString, lineInfo3.get(0).fTypeMask);
		Assert.assertEquals("hi", lineInfo3.get(0).fValue);
		
	}
}
