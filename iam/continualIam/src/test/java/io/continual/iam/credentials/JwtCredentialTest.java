package io.continual.iam.credentials;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.continual.iam.credentials.JwtCredential.InvalidJwtToken;
import io.continual.util.time.Clock;

public class JwtCredentialTest
{
	private final long expTime = 1670936933000L;
	private final long offsetTime = (24 * 60 * 60);		// 1 day
	// Header - {"alg": "HS256","typ": "JWT"}
	// Payload - {"iss": "continual","sub": "continual","aud": "continual","iat": 1670936333,"exp": 1670936933}
	// Key Base64 - 12345678901234567890123456789012345678901234567890
	private final String validJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
				"eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA5MzYzMzMsImV4cCI6MTY3MDkzNjkzM30." +
				"1VBwqpRd1UoRsbcabSzMROxa6xucSRghirmQMVHDYRQ";
	// "aud": ["continual1","continual2"]
	private final String arrAudJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOlsiY29udGludWFsMSIsImNvbnRpbnVhbDIiXSwiaWF0IjoxNjcwOTM2MzMzLCJleHAiOjE2NzA5MzY5MzN9.9vLpOjc6N3QegrEFYhahAEc-2VB_Sr4cOMofUF0iG2Q";

	// InvalidJwtTokens
	// "typ": "ABC"
	private final String invalidTypJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkFCQyJ9.eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA5MzYzMzMsImV4cCI6MTY3MDkzNjkzM30.xpwXEnKFznVLo9nNkVFOOzRGW1fno-BRzwFXHFi19p8";
	// "alg": "HS384"
	private final String invalidAlgJwtToken = "eyJhbGciOiJIUzM4NCIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA5MzYzMzMsImV4cCI6MTY3MDkzNjkzM30.pgSE4_A79iNGYAKru67xHp1OpWBuD7KfFIBU3XbECcjFsRfpO5rxVt9IyBf8kVQo";
	// {"alg": "HS384","typ": "ABC"}
	private final String invalidAlgTypJwtToken = "eyJhbGciOiJIUzM4NCIsInR5cCI6IkFCQyJ9.eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA5MzYzMzMsImV4cCI6MTY3MDkzNjkzM30.iSIcvhEJxxPNsFfxWfLAHZK_iohTpqROHJH5WQIMJiAO_zJgWxmqUD3jLddeLUtD";
	// "sub": ""
	private final String noSubJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiIiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA5MzYzMzMsImV4cCI6MTY3MDkzNjkzM30.vjjcXtNMVKkVA0_65kcahsnmJWfcQ8RuEU3YpzNHVSg";
	// no typ
	private final String invalidJsonJwtToken = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA5MzYzMzMsImV4cCI6MTY3MDkzNjkzM30.2YvAJpzJiGllA_2vf4yHPba73tN2w0U-QBs5DOtrYEg";

	@Before
	public void setUp ()
	{
		// setup a test clock because this test relies on time comparisons. 
		// Clock time set to 1 day before expiry.
		Clock.useNewTestClock ().set ( expTime - offsetTime );
	}

	@Test
	public void testFromHeaderValid ()
	{
		final String authHeader = "Bearer " + validJwtToken;
		try {
			Assert.assertNotNull ( JwtCredential.fromHeader( authHeader ) );
		} catch (InvalidJwtToken e) {
			Assert.fail ( "Expected to generate credential. " + e.getMessage() );
		}
	}

	@Test( expected = InvalidJwtToken.class )
	public void testFromHeaderInValid1 () throws InvalidJwtToken
	{
		final String authHeader = "Bearer " + validJwtToken + " abc";
		JwtCredential.fromHeader( authHeader );
	}

	@Test( expected = InvalidJwtToken.class )
	public void testFromHeaderInvalid2 () throws InvalidJwtToken
	{
		JwtCredential.fromHeader( validJwtToken );
	}

	@Test( expected = InvalidJwtToken.class )
	public void testFromHeaderInvalid3 () throws InvalidJwtToken
	{
		JwtCredential.fromHeader( null );
	}

	@Test
	public void testConstructor ()
	{
		try {
			final JwtCredential jwtCred = new JwtCredential ( validJwtToken );
			Assert.assertEquals ( validJwtToken , jwtCred.toBearerString() );
		} catch (InvalidJwtToken e) {
			Assert.fail ( "Expected to create instance. " + e.getMessage() );
		}
	}

	@Test
	public void testConstructorAudArr ()
	{
		try {
			final JwtCredential jwtCred = new JwtCredential ( arrAudJwtToken );
			Assert.assertEquals ( arrAudJwtToken , jwtCred.toBearerString() );
		} catch (InvalidJwtToken e) {
			Assert.fail ( "Expected to create instance. " + e.getMessage() );
		}
	}

	@Test( expected = InvalidJwtToken.class )
	public void testConstructorInvalidType () throws InvalidJwtToken
	{
		new JwtCredential ( invalidTypJwtToken );
	}

	@Test( expected = InvalidJwtToken.class )
	public void testConstructorInvalidAlg () throws InvalidJwtToken
	{
		new JwtCredential ( invalidAlgJwtToken );
	}

	@Test( expected = InvalidJwtToken.class )
	public void testConstructorInvalidAlgTyp () throws InvalidJwtToken
	{
		new JwtCredential ( invalidAlgTypJwtToken );
	}

	@Test( expected = InvalidJwtToken.class )
	public void testConstructorNoSub () throws InvalidJwtToken
	{
		new JwtCredential ( noSubJwtToken );
	}

	@Test( expected = InvalidJwtToken.class )
	public void testConstructorNoSignature () throws InvalidJwtToken
	{
		final String noSigToken = validJwtToken.substring( 0 , validJwtToken.lastIndexOf(".") ); 
		new JwtCredential ( noSigToken );
	}

	@Test( expected = InvalidJwtToken.class )
	public void testConstructorInvalidJson () throws InvalidJwtToken
	{
		new JwtCredential ( invalidJsonJwtToken );
	}

	@Test
	public void testGetSignedContent ()
	{
		final String expect = validJwtToken.substring( 0 , validJwtToken.lastIndexOf(".") );
		try {
			final JwtCredential jwtCred = new JwtCredential ( validJwtToken );
			Assert.assertEquals ( expect , jwtCred.getSignedContent() );
		} catch (InvalidJwtToken e) {
			Assert.fail ( "Expected to create instance. " + e.getMessage() );
		}
	}

	@Test
	public void testGetSignature ()
	{
		final String expect = validJwtToken.substring( validJwtToken.lastIndexOf(".") + 1 );
		try {
			final JwtCredential jwtCred = new JwtCredential ( validJwtToken );
			Assert.assertEquals ( expect , jwtCred.getSignature() );
		} catch (InvalidJwtToken e) {
			Assert.fail ( "Expected to create instance. " + e.getMessage() );
		}
	}

	@Test
	public void testToString ()
	{
		final String expect = "JWT for continual"; 
		try {
			final JwtCredential jwtCred = new JwtCredential ( validJwtToken );
			Assert.assertEquals ( expect , jwtCred.toString() );
		} catch (InvalidJwtToken e) {
			Assert.fail ( "Expected to create instance. " + e.getMessage() );
		}
	}

	@Test
	public void testGetSubject ()
	{
		final String expect = "continual";
		try {
			final JwtCredential jwtCred = new JwtCredential ( validJwtToken );
			Assert.assertEquals ( expect , jwtCred.getSubject() );
		} catch (InvalidJwtToken e) {
			Assert.fail ( "Expected to create instance. " + e.getMessage() );
		}
	}

	@Test
	public void testGetIssuer ()
	{
		final String expect = "continual";
		try {
			final JwtCredential jwtCred = new JwtCredential ( validJwtToken );
			Assert.assertEquals ( expect , jwtCred.getIssuer() );
		} catch (InvalidJwtToken e) {
			Assert.fail ( "Expected to create instance. " + e.getMessage() );
		}
	}

	@Test
	public void testIsForAudience ()
	{
		final String expect = "continual";
		try {
			final JwtCredential jwtCred = new JwtCredential ( validJwtToken );
			Assert.assertTrue ( jwtCred.isForAudience( expect ) );
		} catch (InvalidJwtToken e) {
			Assert.fail ( "Expected to create instance. " + e.getMessage() );
		}
	}

	@Test
	public void testGetSigningAlgorithm ()
	{
		final String expect = "HS256";
		try {
			final JwtCredential jwtCred = new JwtCredential ( validJwtToken );
			Assert.assertEquals ( expect , jwtCred.getSigningAlgorithm() );
		} catch (InvalidJwtToken e) {
			Assert.fail ( "Expected to create instance. " + e.getMessage() );
		}
	}

	@Test
	public void testGetExpiration ()
	{
		final long expect = expTime / 1000;
		try {
			final JwtCredential jwtCred = new JwtCredential ( validJwtToken );
			Assert.assertEquals ( expect , jwtCred.getExpiration() );
		} catch (InvalidJwtToken e) {
			Assert.fail ( "Expected to create instance. " + e.getMessage() );
		}
	}

	@Test
	public void testIsExpiredFalse ()
	{
		try {
			final JwtCredential jwtCred = new JwtCredential ( validJwtToken );
			Assert.assertFalse ( jwtCred.isExpired() );
		} catch (InvalidJwtToken e) {
			Assert.fail ( "Expected to create instance. " + e.getMessage() );
		}
	}

	@Test
	public void testIsExpiredTrue ()
	{
		try {
			final JwtCredential jwtCred = new JwtCredential ( validJwtToken );

			// setup a test clock because this test relies on time comparisons 
			Clock.useNewTestClock ().set ( expTime + offsetTime );

			Assert.assertTrue ( jwtCred.isExpired() );
		} catch (InvalidJwtToken e) {
			Assert.fail ( "Expected to create instance. " + e.getMessage() );
		}
	}

	@Test( expected = InvalidJwtToken.class )
	public void testConstructorExpired () throws InvalidJwtToken
	{
		// setup a test clock because this test relies on time comparisons 
		Clock.useNewTestClock ().set ( expTime + offsetTime );

		new JwtCredential ( validJwtToken );
	}

	@After
	public void tearDown ()
	{
		Clock.useNewTestClock();
	}
}
