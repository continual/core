package io.continual.util.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.continual.util.data.StringUtils.charSelector;
import io.continual.util.data.StringUtils.fieldInfo;
import io.continual.util.data.StringUtils.valueInfo;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.impl.nvReadableStack;

public class StringUtilsTest {
	
	@Test
    public void emptyIfNull(){
		Assert.assertEquals("", StringUtils.emptyIfNull(null));
    }
	
	@Test
    public void emptyIfNull_Not(){
		Assert.assertEquals("test", StringUtils.emptyIfNull("test"));
    }
	
	@Test
    public void substringTo_NotContainsDemiliter(){
		Assert.assertEquals("test", StringUtils.substringTo("test", '-'));
    }
	
	@Test
    public void substringTo_ContainsDemiliter(){
		Assert.assertEquals("test", StringUtils.substringTo("test-me", '-'));
    }
	
	@Test
    public void isOneOf(){
		Assert.assertEquals(2, StringUtils.isOneOf('a', new char[] {'r','-','a'}));
    }

	@Test
    public void isOneOf_none(){
		Assert.assertEquals(-1, StringUtils.isOneOf('-', new char[] {'r','t','0'}));
    }
	
	@Test
    public void indexOfAnyOf(){
		Assert.assertEquals(7, StringUtils.indexOfAnyOf("test text", new char[] {'r','-','x'}));
    }
	
	@Test
    public void indexOfAnyOf_none(){
		Assert.assertEquals(-1, StringUtils.indexOfAnyOf("test text", new char[] {'r','-','p'}));
    }
	
	@Test
    public void indexOfAnyOf_fromIndex(){
		Assert.assertEquals(3, StringUtils.indexOfAnyOf("test text", new char[] {'r','-','t'}, 2));
    }
	
	@Test
    public void toFirstUpperRestLower(){
		Assert.assertEquals("Test text", StringUtils.toFirstUpperRestLower("test TEXT"));
    }
	
	@Test
    public void toFirstUpperRestLower_null(){
		Assert.assertEquals(null, StringUtils.toFirstUpperRestLower(null));
    }
	
	@Test
    public void toFirstUpper(){
		Assert.assertEquals("Test TEXT", StringUtils.toFirstUpper("test TEXT"));
    }
	
	@Test
    public void toFirstUpper_null(){
		Assert.assertEquals(null, StringUtils.toFirstUpper(null));
    }
	
	@Test
    public void toFirstUpper_empty(){
		Assert.assertEquals("", StringUtils.toFirstUpper(""));
    }
	
	@Test
    public void toFirstUpper_emptyOneChar(){
		Assert.assertEquals("A", StringUtils.toFirstUpper("a"));
    }
	
	@Test
    public void dequote_withoutQuote(){
		Assert.assertEquals("test TEXT", StringUtils.dequote("test TEXT"));
    }
	
	@Test
    public void dequote(){
		Assert.assertEquals("test data TEXT", StringUtils.dequote("\"test data TEXT\""));
    }
	
	@Test
    public void dequote_onlyOneSide(){
		Assert.assertEquals("\"test data TEXT", StringUtils.dequote("\"test data TEXT"));
    }
	
	@Test
    public void splitList(){
		Assert.assertArrayEquals(new String[] {"data","test"}, StringUtils.splitList("data;test"));
    }
	
	@Test
    public void splitList_quote(){
		Assert.assertArrayEquals(new String[] {"data","test"}, StringUtils.splitList("data*test", new char[] {'*'}, new char[] {}));
    }
	
	@Test
    public void splitListToList(){
		Assert.assertEquals(Arrays.asList("data","test"), StringUtils.splitListToList("data;test"));
    }
	
	@Test
    public void splitListToList_quote(){
		Assert.assertEquals(Arrays.asList("data","test"), StringUtils.splitListToList("data?test", new char[] {'?'}, new char[] {}));
    }
	
	@Test
    public void splitListToList_quote_vifValueNull(){
		Assert.assertEquals(Arrays.asList("","test"), StringUtils.splitListToList(" test", new char[] {' '}, new char[] {'1'}));
    }
	
	@Test
    public void getLeadingValue(){
		valueInfo expectedValueInfo = new valueInfo("istanbul", 9);
		valueInfo result = StringUtils.getLeadingValue("istanbul,ankara");
		Assert.assertEquals(expectedValueInfo.fValue, result.fValue);
		Assert.assertEquals(expectedValueInfo.fNextFieldAt, result.fNextFieldAt);
    }
	
	@Test
    public void getLeadingValue_delimeter(){
		valueInfo expectedValueInfo = new valueInfo("istanbul", 9);
		valueInfo result = StringUtils.getLeadingValue("istanbul*ankara", '1', '*');
		Assert.assertEquals(expectedValueInfo.fValue, result.fValue);
		Assert.assertEquals(expectedValueInfo.fNextFieldAt, result.fNextFieldAt);
    }
	
	@Test
    public void getLeadingValue_delimeterArray(){
		valueInfo expectedValueInfo = new valueInfo("istanbul", 9);
		valueInfo result = StringUtils.getLeadingValue("istanbul*anka1ra", new char[] {'1'}, new char[] {'*'});
		Assert.assertEquals(expectedValueInfo.fValue, result.fValue);
		Assert.assertEquals(expectedValueInfo.fNextFieldAt, result.fNextFieldAt);
    }
	
	@Test
    public void getLeadingValue_delimeterArray_emptyString(){
		valueInfo result = StringUtils.getLeadingValue("", new char[] {'1'}, new char[] {'*'});
		Assert.assertEquals(null, result.fValue);
		Assert.assertEquals(-1, result.fNextFieldAt);
    }
	
	@Test
    public void getLeadingValue_delimeterArray_emptyDelimeter(){
		valueInfo result = StringUtils.getLeadingValue(" test", new char[] {'1'}, new char[] {' '});
		Assert.assertEquals(null, result.fValue);
		Assert.assertEquals(1, result.fNextFieldAt);
    }
	
	@Test
    public void getLeadingValue_delimeterArray_quoted(){
		valueInfo result = StringUtils.getLeadingValue("ex", 'e', '*');
		Assert.assertEquals(null, result.fValue);
		Assert.assertEquals(-1, result.fNextFieldAt);
    }
	
	@Test
    public void getLeadingValue_delimeterArray2(){
		valueInfo expectedValueInfo = new valueInfo("istan", 11);
		valueInfo result = StringUtils.getLeadingValue("-istan-bul*ankara", new char[] {'-'}, new char[] {'*'});
		Assert.assertEquals(expectedValueInfo.fValue, result.fValue);
		Assert.assertEquals(expectedValueInfo.fNextFieldAt, result.fNextFieldAt);
    }
	
	@Test
    public void getLeadingValue_delimeterArray3(){
		valueInfo expectedValueInfo = new valueInfo(null, -1);
		valueInfo result = StringUtils.getLeadingValue("\\\\\\test", new char[] {'\\'}, new char[] {' '});
		Assert.assertEquals(expectedValueInfo.fValue, result.fValue);
		Assert.assertEquals(expectedValueInfo.fNextFieldAt, result.fNextFieldAt);
    }	
	
	@Test
    public void getLeadingValue_delimeterArray4(){
		valueInfo expectedValueInfo = new valueInfo("te", -1);
		valueInfo result = StringUtils.getLeadingValue("\\te\\st", new char[] {'\\'}, new char[] {'-'});
		Assert.assertEquals(expectedValueInfo.fValue, result.fValue);
		Assert.assertEquals(expectedValueInfo.fNextFieldAt, result.fNextFieldAt);
    }
	
	@Test
    public void split(){
		List<fieldInfo> expected = new ArrayList<>();
		expected.add(new fieldInfo("horse", 0));
		List<fieldInfo> result = StringUtils.split("horse", '(', '-');
		
		Assert.assertNotNull(expected);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(expected.get(0).fValue, result.get(0).fValue);
		Assert.assertEquals(expected.get(0).fStartsAt, result.get(0).fStartsAt);
    }
	
	@Test
    public void split_toString(){
		List<fieldInfo> expected = new ArrayList<>();
		expected.add(new fieldInfo("horse", 0));
		List<fieldInfo> result = StringUtils.split("horse", '(', '-');
		
		Assert.assertNotNull(expected);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals("[horse [0]]", result.toString());
    }
	
	@Test
    public void split_moreThanOnceDemiliter(){
		List<fieldInfo> expected = new ArrayList<>();
		expected.add(new fieldInfo("hor", 0));
		expected.add(new fieldInfo("se", 4));
		expected.add(new fieldInfo("plays", 7));
		List<fieldInfo> result = StringUtils.split("hor-se-plays", '(', '-');
		
		Assert.assertNotNull(expected);
		Assert.assertEquals(3, result.size());
		Assert.assertEquals(expected.get(0).fValue, result.get(0).fValue);
		Assert.assertEquals(expected.get(0).fStartsAt, result.get(0).fStartsAt);
		Assert.assertEquals(expected.get(1).fValue, result.get(1).fValue);
		Assert.assertEquals(expected.get(1).fStartsAt, result.get(1).fStartsAt);
		Assert.assertEquals(expected.get(2).fValue, result.get(2).fValue);
		Assert.assertEquals(expected.get(2).fStartsAt, result.get(2).fStartsAt);
    }
	
	@Test
    public void indexOf(){
		charSelector wLetterSelector = new charSelector() {			
			@Override
			public boolean select(Character c) {
				return c.equals('w');
			}
		};
		Assert.assertEquals(3, StringUtils.indexOf("flowers good", wLetterSelector ));
    }
	
	@Test
    public void indexOf_none(){
		charSelector wLetterSelector = new charSelector() {			
			@Override
			public boolean select(Character c) {
				return c.equals('w');
			}
		};
		Assert.assertEquals(-1, StringUtils.indexOf("good", wLetterSelector ));
    }
	
	@Test
    public void evaluate(){
		NvReadable nv = new nvReadableStack();
		Assert.assertEquals("flower", StringUtils.evaluate(nv ,"flower"));
    }
	
	@Test
    public void limitLengthTo(){
		Assert.assertEquals("flowe", StringUtils.limitLengthTo("flowers good", 5));
    }
	
	@Test
    public void limitLengthTo_short(){
		Assert.assertEquals("flowers good", StringUtils.limitLengthTo("flowers good", 45));
    }

	@Test
    public void isEmptyTest(){
		Assert.assertEquals(true, StringUtils.isEmpty(null));
		Assert.assertEquals(true, StringUtils.isEmpty(""));
		Assert.assertEquals(false, StringUtils.isEmpty("fun"));
	}

	@Test
	public void isNotEmptyTest(){
		Assert.assertEquals(true, StringUtils.isNotEmpty("fun"));
		Assert.assertEquals(false, StringUtils.isNotEmpty(null));
	}
}
