package io.continual.iam.impl.common;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.continual.iam.credentials.ApiKeyCredential;
import junit.framework.TestCase;

public class ApiKeyAuthHelperTest extends TestCase
{
	private TestHeaderReader thr;

	@Before
	public void setUp ()
	{
		thr = new TestHeaderReader ();
		thr.put ( ApiKeyAuthHelper.kDefault_AuthLineHeader , "continual:continual" );
		thr.put ( ApiKeyAuthHelper.kDefault_DateLineHeader , new SimpleDateFormat ( 
				SignedContentReader.kPreferredDateFormat , Locale.US ).format( new java.util.Date () ) );
		thr.put ( ApiKeyAuthHelper.kDefault_MagicLineHeader , "continual" );
	}

	@Test
	public void testReadApiKeyCredential_Valid ()
	{
		final ApiKeyCredential akc = ApiKeyAuthHelper.readApiKeyCredential ( new JSONObject () , thr , "continual" );
		assertNotNull ( akc );
		assertEquals ( "continual" , akc.getApiKey () );
		assertEquals ( "continual" , akc.getSignature () );
		assertNotNull ( akc.getContent () );
	}

	@Test
	public void testReadApiKeyCredential_NoAuth ()
	{
		final TestHeaderReader invalidThr = new TestHeaderReader ();
		invalidThr.put ( ApiKeyAuthHelper.kDefault_AuthLineHeader , "continual:continual" );
		assertNull ( ApiKeyAuthHelper.readApiKeyCredential ( new JSONObject () , invalidThr , null ) );
	}

	@Test
	public void testReadApiKeyCredential_SignedContent ()
	{
		assertNull ( ApiKeyAuthHelper.readApiKeyCredential ( new JSONObject () , new TestHeaderReader () , null ) );
	}

	@Test
	public void testReadApiKeyCredential_InvalidAuth ()
	{
		final TestHeaderReader invalidThr = new TestHeaderReader ();
		invalidThr.put ( ApiKeyAuthHelper.kDefault_AuthLineHeader , "continual_continual" );
		invalidThr.put ( ApiKeyAuthHelper.kDefault_DateLineHeader , new SimpleDateFormat ( 
				SignedContentReader.kPreferredDateFormat , Locale.US ).format( new java.util.Date () ) );
		assertNull ( ApiKeyAuthHelper.readApiKeyCredential ( new JSONObject () , invalidThr , null ) );
	}

	@Test
	public void testEmptyConstructor ()
	{
		assertNotNull ( new ApiKeyAuthHelper () );
	}

	@After
	public void tearDown ()
	{
		thr = null;
	}

	private static class TestHeaderReader implements HeaderReader
	{
		Map<String , String> keyValuePair = new HashMap<>();
		public void put ( String key , String value )
		{
			keyValuePair.put ( key , value );
		}
		@Override
		public String getFirstHeader(String header) 
		{
			return keyValuePair.get ( header );
		}
	}
}
