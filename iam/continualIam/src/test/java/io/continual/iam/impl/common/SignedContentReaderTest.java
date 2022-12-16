package io.continual.iam.impl.common;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import junit.framework.TestCase;

public class SignedContentReaderTest extends TestCase
{
	@Test
	public void testGetSignedContent_NullParams ()
	{
		assertNull ( SignedContentReader.getSignedContent( null , null , null ) );
	}

	@Test
	public void testGetSignedContent_MaxParams ()
	{
		final String httpDateString = new SimpleDateFormat ( SignedContentReader.kPreferredDateFormat , Locale.US )
											.format( new java.util.Date () );
		assertNotNull ( SignedContentReader.getSignedContent( httpDateString , null , "apimagic" , "productTag" ) );
	}

	@Test
	public void testGetSignedContent_InvalidDT ()
	{
		final String httpDateString = "invalid";
		assertNull ( SignedContentReader.getSignedContent( httpDateString , null , "productTag" ) );
	}

	@Test
	public void testGetSignedContent_TimeDiff ()
	{
		// 90 minutes before
		final String httpDateString = new SimpleDateFormat ( SignedContentReader.kPreferredDateFormat , Locale.US )
				.format( new java.util.Date ( System.currentTimeMillis() - ( 9000 * 60 * 10 ) ) );
		assertNull ( SignedContentReader.getSignedContent( httpDateString , null , null ) );
	}

	@Test
	public void testGetSignedContent_ApiParams ()
	{
		final TestApiRequestData tard = new TestApiRequestData ();
		final String dateString = new SimpleDateFormat ( SignedContentReader.kPreferredDateFormat , Locale.US )
				.format( new java.util.Date () );
		tard.put ( "Date" , new String[] { dateString } );
		assertNotNull ( SignedContentReader.getSignedContent ( tard , "Date" , "Date" , "Date" ) );
	}

	@Test
	public void testEmptyConstructor ()
	{
		assertNotNull ( new SignedContentReader () );
	}

	private static class TestApiRequestData implements SignedContentReader.ApiRequestData
	{
		Map<String , String[]> keyValuePair = new HashMap<>();
		public void put ( String key , String[] arrValue )
		{
			keyValuePair.put ( key , arrValue );
		}
		@Override
		public String getFirstValue ( String header ) 
		{
			return keyValuePair.get ( header )[0];
		}
		@Override
		public String[] getValuesArray ( String fHeaderAuth )
		{
			return keyValuePair.get ( fHeaderAuth );
		}
	}
}
