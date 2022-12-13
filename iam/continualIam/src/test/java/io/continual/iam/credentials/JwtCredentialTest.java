package io.continual.iam.credentials;

import org.junit.Assert;
import org.junit.Test;

import io.continual.iam.credentials.JwtCredential.InvalidJwtToken;
import io.continual.util.time.Clock;
import io.continual.util.time.Clock.TestClock;

public class JwtCredentialTest
{
	// Header - {"alg": "HS256","typ": "JWT"}
	// Payload - {"iss": "continual","sub": "continual","aud": "continual","iat": 1670770128,"exp": 96707707280}
	// Key Base64 - 12345678901234567890123456789012345678901234567890
	private final String validJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
				"eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA3NzAxMjgsImV4cCI6OTY3MDc3MDcyODB9." +
				"I_fkxgSPyfMY1F5dCoO20qQYQgB8v5Fkm9KWMxx67qI";
	// "aud": "{continual1,continual2}"
	private final String arrAudJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOlsiY29udGludWFsMSIsImNvbnRpbnVhbDIiXSwiaWF0IjoxNjcwNzcwMTI4LCJleHAiOjk2NzA3NzA3MjgwfQ.DT1MGe5XMePpkupdpw1LJvUkHZ-7WMPr8u3C6Y51bqQ";

	// InvalidJwtToken
	// "exp": 1670770129
	private final String expiredJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA3NzAxMjgsImV4cCI6MTY3MDc3MDEyOX0.dKEI7IpDu1FmxF2WjJXuLl-PI9M1tjGb_HH_1255GQw";
	// "typ": "ABC"
	private final String invalidTypeJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkFCQyJ9.eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA3NzAxMjgsImV4cCI6OTY3MDc3MDcyODB9.tWM9WzsNVutguAWYFojlPPEOxEUArG5WuUIAVoEEP5k";
	// "sub": ""
	private final String noSubJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiIiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA3NzAxMjgsImV4cCI6OTY3MDc3MDcyODB9.Ne12lDTuewVIwD4WlsMnaNi_bA07C7xx8-Bmq1-r8tU";
	// no typ
	private final String invalidJsonJwtToken = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA3NzAxMjgsImV4cCI6OTY3MDc3MDcyODB9.y3WN_qCCnm9SwFPz2xwtuLmqwvUbP2kabJL6QI4Sefk";

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
	public void testConstructorExpired () throws InvalidJwtToken
	{
		// setup a test clock because this test relies on time comparisons 
		Clock.useNewTestClock ().set ( 1670885435000L );

		new JwtCredential ( expiredJwtToken );
	}

	@Test( expected = InvalidJwtToken.class )
	public void testConstructorInvalidType () throws InvalidJwtToken
	{
		new JwtCredential ( invalidTypeJwtToken );
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
		final long expect = 96707707280L;
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
}
