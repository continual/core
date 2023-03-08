package io.continual.util.data;

import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import io.continual.util.data.TypeConvertor.conversionError;

public class TypeConvertorTest {
	
	@Test
    public void convertToBoolean(){
		Assert.assertTrue(TypeConvertor.convertToBoolean("true"));
    }
	
	@Test
    public void convertToBooleanBroad(){
		Assert.assertTrue(TypeConvertor.convertToBooleanBroad("true"));
    }
	
	@Test
    public void convertToBooleanBroad_null(){
		Assert.assertFalse(TypeConvertor.convertToBooleanBroad(null));
    }
	
	@Test
    public void convertToBooleanBroad_yes(){
		Assert.assertTrue(TypeConvertor.convertToBooleanBroad("yes"));
    }
	
	@Test
    public void convertToBooleanBroad_on(){
		Assert.assertTrue(TypeConvertor.convertToBooleanBroad("on"));
    }
	
	@Test
    public void convertToBooleanBroad_one(){
		Assert.assertTrue(TypeConvertor.convertToBooleanBroad("1"));
    }
	
	@Test
    public void convertToBooleanBroad_y(){
		Assert.assertTrue(TypeConvertor.convertToBooleanBroad("y"));
    }
	
	@Test
    public void convertToBooleanBroad_checked(){
		Assert.assertTrue(TypeConvertor.convertToBooleanBroad("checked"));
    }
	
	@Test
    public void convertToBooleanBroad_test(){
		Assert.assertFalse(TypeConvertor.convertToBooleanBroad("test"));
    }
	
	@Test
    public void convertToBoolean_int(){
		Assert.assertTrue(TypeConvertor.convertToBoolean(5));
    }
	
	@Test
    public void convertToBoolean_int2(){
		Assert.assertFalse(TypeConvertor.convertToBoolean(0));
    }
	
	@Test
    public void convertToBoolean_long(){
		Assert.assertTrue(TypeConvertor.convertToBoolean(5l));
    }
	
	@Test
    public void convertToBoolean_long2(){
		Assert.assertFalse(TypeConvertor.convertToBoolean(0l));
    }
	
	@Test
    public void convertToInt() throws conversionError{
		Assert.assertEquals(0, TypeConvertor.convertToInt(null));
    }
	
	@Test(expected = conversionError.class)
    public void convertToInt_wrongText() throws conversionError{
		TypeConvertor.convertToInt("one");
    }
	
	@Test
    public void convertToInt_correct() throws conversionError{
		Assert.assertEquals(12, TypeConvertor.convertToInt("12"));
    }
	
	@Test
    public void convertToInt_errval() throws conversionError{
		Assert.assertEquals(15, TypeConvertor.convertToInt("15", 0));
    }
	
	@Test
    public void convertToInt_errval_wrong() throws conversionError{
		Assert.assertEquals(5, TypeConvertor.convertToInt("two", 5));
    }
	
	@Test
    public void convertToLong() throws conversionError{
		Assert.assertEquals(0, TypeConvertor.convertToLong(null));
    }
	
	@Test(expected = conversionError.class)
    public void convertToLong_wrongFormat() throws conversionError{
		TypeConvertor.convertToLong("four");
    }
	
	@Test
    public void convertToLong_correct() throws conversionError{
		Assert.assertEquals(447, TypeConvertor.convertToLong("447"));
    }
	
	@Test
    public void convertToLong_errval() throws conversionError{
		Assert.assertEquals(447, TypeConvertor.convertToLong("447", 8));
    }
	
	@Test
    public void convertToLong_errval_wrong() throws conversionError{
		Assert.assertEquals(12, TypeConvertor.convertToLong("four", 12));
    }
	
	@Test
    public void convertToShort() throws conversionError{
		Assert.assertEquals(0, TypeConvertor.convertToShort(null));
    }
	
	@Test(expected = conversionError.class)
    public void convertToShort_wrongFormat() throws conversionError{
		TypeConvertor.convertToShort("four");
    }
	
	@Test
    public void convertToShort_correct() throws conversionError{
		Assert.assertEquals(447, TypeConvertor.convertToShort("447"));
    }
	
	@Test
    public void convertToShort_errval() throws conversionError{
		Assert.assertEquals(447, TypeConvertor.convertToShort("447", 8), 0);
    }
	
	@Test
    public void convertToShort_errval_wrong() throws conversionError{
		Assert.assertEquals(12, TypeConvertor.convertToShort("four", 12), 0);
    }
	
	@Test
    public void convertToDouble() throws conversionError{
		Assert.assertEquals(0, TypeConvertor.convertToDouble(null), 0);
    }
	
	@Test(expected = conversionError.class)
    public void convertToDouble_wrongFormat() throws conversionError{
		TypeConvertor.convertToDouble("four");
    }
	
	@Test
    public void convertToDouble_correct() throws conversionError{
		Assert.assertEquals(447, TypeConvertor.convertToDouble("447"), 0);
    }
	
	@Test
    public void convertToFloat() throws conversionError{
		Assert.assertEquals(0, TypeConvertor.convertToFloat(null), 0);
    }
	
	@Test(expected = conversionError.class)
    public void convertToFloat_wrongFormat() throws conversionError{
		TypeConvertor.convertToFloat("four");
    }
	
	@Test
    public void convertToFloat_correct() throws conversionError{
		Assert.assertEquals(447, TypeConvertor.convertToFloat("447"), 0);
    }
	
	@Test
    public void convertToFloat_errval() throws conversionError{
		Assert.assertEquals(447, TypeConvertor.convertToFloat("447", 8), 0);
    }
	
	@Test
    public void convertToFloat_errval_wrong() throws conversionError{
		Assert.assertEquals(12, TypeConvertor.convertToFloat("four", 12), 0);
    }
	
	@Test
    public void convertToDouble_errval() throws conversionError{
		Assert.assertEquals(3, TypeConvertor.convertToDouble(null, 3), 0);
    }
	
	@Test
    public void convertToDouble_errval2() throws conversionError{
		Assert.assertEquals(447, TypeConvertor.convertToDouble("447", 8), 0);
    }
	
	@Test
    public void convertToDouble_errval_wrong() throws conversionError{
		Assert.assertEquals(12, TypeConvertor.convertToDouble("four", 12), 0);
    }
	
	@Test(expected = conversionError.class)
    public void convertToCharacter() throws conversionError{
		TypeConvertor.convertToCharacter(null);
    }
	
	@Test(expected = conversionError.class)
    public void convertToCharacter_empty() throws conversionError{
		TypeConvertor.convertToCharacter("");
    }
	
	@Test
    public void convertToCharacter_correct() throws conversionError{
		Assert.assertEquals(55, TypeConvertor.convertToCharacter("7"));
    }
	
	@Test
    public void convertToCharacter_errval() throws conversionError{
		Assert.assertEquals(56, TypeConvertor.convertToCharacter("8", '-'), 0);
    }
	
	@Test
    public void convertToCharacter_errval_wrong() throws conversionError{
		Assert.assertEquals(57, TypeConvertor.convertToCharacter("12", '9'), 0);
    }
	
	@Test
    public void convertToDate() {
		Assert.assertEquals(null, TypeConvertor.convertToDate("123"));
    }
	
	@Test
    public void convertToDate_correctFormat() {
		Date actual = TypeConvertor.convertToDate("12/31/2022");
		Date expected = Timestamp.valueOf(LocalDateTime.of(2022, 12, 31, 0, 0, 0));
		Assert.assertEquals(expected.getTime(), actual.getTime());
    }
	
	@Test
	public void dateToIso8601 ()
	{
		final ZonedDateTime zdt = ZonedDateTime.of ( 2022, 12, 31, 0, 0, 0, 0, ZoneId.of ( "UTC" ) );
		final Date d = Date.from ( zdt.toInstant () );
		Assert.assertEquals ( "2022-12-31T00:00:00Z", TypeConvertor.dateToIso8601 ( d ) );
	}
	
	@Test
    public void dateToIso8601_long() {
		long expected = Instant.parse("2022-12-30T21:00:00Z").toEpochMilli();
		Assert.assertEquals("2022-12-30T21:00:00Z", TypeConvertor.dateToIso8601(expected));
    }

	@Test
	public void iso8601ToEpochMs() throws ParseException {
		Assert.assertEquals(1672434000000L, TypeConvertor.iso8601ToEpochMs("2022-12-30T21:00:00.000Z"));
	}

	@Test
	public void iso8601DateOnlyToEpochMs () throws ParseException
	{
		Assert.assertEquals ( 1672358400000L, TypeConvertor.iso8601ToEpochMs ( "2022-12-30" ) );
	}

	@Test(expected = ParseException.class)
    public void iso8601ToEpochMs_wrongFormat() throws ParseException {
		TypeConvertor.iso8601ToEpochMs("test");
    }
	
	@Test
    public void convert() throws conversionError {
		Assert.assertArrayEquals(new byte[] {12, 99, 3} , TypeConvertor.convert(new int[] { 12, 99, 3 }));
    }
	
	@Test(expected = conversionError.class)
    public void convert_wrongFormat() throws conversionError {
		TypeConvertor.convert(new int[] { 555 });
    }
	
	@Test(expected = conversionError.class)
    public void convert_wrongFormat2() throws conversionError {
		TypeConvertor.convert(new int[] { -200 });
    }
	
	@Test
    public void convert_byte() {
		Assert.assertArrayEquals(new int[] { 74, 251, 103 } , TypeConvertor.convert(new byte[] { 74, -5, 103 }));
    }
	
	@Test
    public void convertToString() throws conversionError {
		Assert.assertEquals("38", TypeConvertor.convertToString(new int[] { 56 }, 0, 1));
    }
	
	@Test(expected = conversionError.class)
    public void convertToString_wrongFormat() throws conversionError {
		TypeConvertor.convertToString(new int[] { -200 }, 0, 20);
    }
	
	@Test(expected = conversionError.class)
    public void convertToString_wrongFormat2() throws conversionError {
		TypeConvertor.convertToString(new int[] { 300 }, 0, 20);
    }
	
	@Test
    public void nibbleToChar() throws conversionError {
		Assert.assertEquals('9', TypeConvertor.nibbleToChar(9));
    }
	
	@Test
    public void nibbleToChar2() throws conversionError {
		Assert.assertEquals('F', TypeConvertor.nibbleToChar(15));
    }
	
	@Test(expected = conversionError.class)
    public void nibbleToChar_wrongFormat() throws conversionError {
		TypeConvertor.nibbleToChar(20);
    }
	
	@Test(expected = conversionError.class)
    public void nibbleToChar_wrongFormat2() throws conversionError {
		TypeConvertor.nibbleToChar(-4);
    }
	
	@Test
    public void charToNibble() throws conversionError {
		Assert.assertEquals(0, TypeConvertor.charToNibble('0'));
    }
	@Test
    public void charToNibble2() throws conversionError {
		Assert.assertEquals(5, TypeConvertor.charToNibble('5'));
    }
	@Test
    public void charToNibble3() throws conversionError {
		Assert.assertEquals(9, TypeConvertor.charToNibble('9'));
    }
	@Test
    public void charToNibble4() throws conversionError {
		Assert.assertEquals(10, TypeConvertor.charToNibble('a'));
    }
	@Test
    public void charToNibble5() throws conversionError {
		Assert.assertEquals(12, TypeConvertor.charToNibble('c'));
    }
	@Test
    public void charToNibble6() throws conversionError {
		Assert.assertEquals(15, TypeConvertor.charToNibble('f'));
    }
	@Test(expected = conversionError.class)
    public void charToNibble7() throws conversionError {
		TypeConvertor.charToNibble('.');
    }
	
	@Test(expected = conversionError.class)
    public void charToNibble_wrongFormat() throws conversionError {
		TypeConvertor.charToNibble('y');
    }
	
	@Test(expected = conversionError.class)
    public void charToNibble_wrongFormat2() throws conversionError {
		TypeConvertor.charToNibble('Y');
    }
	
	@Test
    public void convertToByteArray() throws conversionError {
		Assert.assertArrayEquals(new int[] {16}, TypeConvertor.convertToByteArray("10"));
    }
	
	@Test(expected = conversionError.class)
    public void convertToByteArray_wrongFormat() throws conversionError {
		TypeConvertor.convertToByteArray("2");
    }
	
	@Test
    public void convertToByteArray_errval() throws conversionError {
		Assert.assertArrayEquals(new int[] {16}, TypeConvertor.convertToByteArray("10", new int[] {1}));
    }
	
	@Test
    public void convertToByteArray_errval2() throws conversionError {
		Assert.assertArrayEquals(new int[] {-1}, TypeConvertor.convertToByteArray("7", new int[] {-1}));
    }
	
	@Test
    public void hexToBytes() throws conversionError {
		Assert.assertArrayEquals(new byte[] {22}, TypeConvertor.hexToBytes("16"));
    }
	
	@Test
    public void byteToHex() {
		Assert.assertEquals("03", TypeConvertor.byteToHex(3));
    }
	
	@Test
    public void byteToHex1() {
		Assert.assertEquals("0A", TypeConvertor.byteToHex(10));
    }
	
	@Test
    public void bytesToHex()  {
		Assert.assertEquals("16", TypeConvertor.bytesToHex(new byte[] {22}, 0 , 1));
    }
	
	@Test
    public void stringToHex() {
		Assert.assertEquals("31", TypeConvertor.stringToHex("1"));
    }
	
	@Test
    public void hexBytesToString() throws conversionError {
		Assert.assertEquals("R", TypeConvertor.hexBytesToString("52"));
    }
	
	@Test
    public void urlEncode() {
		Assert.assertEquals(null, TypeConvertor.urlEncode(null));
    }
	
	@Test
    public void urlEncode2() {
		Assert.assertEquals("http%3A%2F%2Fwww.hi.com", TypeConvertor.urlEncode("http://www.hi.com"));
    }
	
	@Test
    public void urlDecode() {
		Assert.assertEquals(null, TypeConvertor.urlDecode(null));
    }
	
	@Test
    public void urlDecode2() {
		Assert.assertEquals("http://www.hi.com", TypeConvertor.urlDecode("http%3A%2F%2Fwww.hi.com"));
    }
	
	@Test
    public void requoteHtmlInputValue() {
		Assert.assertEquals("http://www.hi.com&quot;", TypeConvertor.requoteHtmlInputValue("http://www.hi.com\""));
    }
	
	@Test
    public void encode_basic() {
		Assert.assertEquals("test", TypeConvertor.encode("test", '-'));
    }
	
	@Test
    public void encode() {
		Assert.assertEquals("te--st", TypeConvertor.encode("te-st", '-', new char[]{'*'}, new char[]{'?'}));
    }
	
	@Test
    public void encode2() {
		Assert.assertEquals("te-?st", TypeConvertor.encode("te*st", '-', new char[]{'*'}, new char[]{'?'}));
    }
	
	@Test
    public void encode3() {
		Assert.assertEquals("te---?st", TypeConvertor.encode("te-*st", '-', new char[]{'*'}, new char[]{'?'}));
    }	
	
	@Test
    public void csvEncodeString() {
		Assert.assertEquals("\"te\"\"st\"", TypeConvertor.csvEncodeString("te\"st"));
    }
	
	@Test
    public void csvEncodeString_forceQuote() {
		Assert.assertEquals("\"test\"", TypeConvertor.csvEncodeString("test", true));
    }
	
	@Test
    public void decode_basic() {
		Assert.assertEquals("test", TypeConvertor.decode("te-st", '-'));
    }
	
	@Test
    public void decode() {
		Assert.assertEquals("te-st", TypeConvertor.decode("te--st", '-', new char[]{'*'}, new char[]{'?'}));
    }
	
	@Test
    public void decode2() {
		Assert.assertEquals("test-", TypeConvertor.decode("test-", '-', new char[]{'*'}, new char[]{'?'}));
    }
	
	@Test
    public void decode3() {
		Assert.assertEquals("t*est", TypeConvertor.decode("t-?est", '-', new char[]{'*'}, new char[]{'?'}));
    }
	
	@Test
    public void base64Encode() {
		Assert.assertEquals("dGVzdA==", TypeConvertor.base64Encode("test"));
    }
		
	@Test
    public void base64Encode_byteArray() {
		Assert.assertEquals("DA==", TypeConvertor.base64Encode(new byte[] { 12 }));
    }
	
	@Test
    public void base64Encode_byteArray_perLine() {
		Assert.assertEquals("DA=\n=", TypeConvertor.base64Encode(new byte[] { 12 }, 3));
    }
	
	@Test
    public void base64UrlEncode() {
		Assert.assertEquals("aHR0cDovL2EuY29t", TypeConvertor.base64UrlEncode("http://a.com"));
    }
	
	@Test
    public void base64UrlEncode2() {
		Assert.assertEquals("DA", TypeConvertor.base64UrlEncode(new byte[] { 12 }));
    }
	
	@Test
    public void base64Decode() {
		Assert.assertArrayEquals(new byte[] { 12 }, TypeConvertor.base64Decode("DA=="));
    }
	
	@Test
    public void base64UrlDecode() {
		Assert.assertArrayEquals(new byte[] { 12 }, TypeConvertor.base64UrlDecode("DA"));
    }
	
	@Test
    public void base64UrlDecode2() {
		Assert.assertArrayEquals(new byte[] { 12, 0 }, TypeConvertor.base64UrlDecode("DAB"));
    }
	
	@Test
    public void base64UrlDecode3() {
		Assert.assertArrayEquals(new byte[] { 12, 0, 84 }, TypeConvertor.base64UrlDecode("DABU"));
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void base64UrlDecode_wrongFormat() {
		TypeConvertor.base64UrlDecode("D");
    }
	
	@Test
    public void bytesToHexString() {
		Assert.assertEquals("01", TypeConvertor.bytesToHexString(new byte[] {1}));
    }
	
	@Test
    public void bytesToHexString2() {
		Assert.assertEquals("7f", TypeConvertor.bytesToHexString(new byte[] {127}));
    }
	
	@Test
    public void hexStringToBytes() {
		Assert.assertArrayEquals(new byte[] { 22 }, TypeConvertor.hexStringToBytes("16"));
    }
	
	
	
}
