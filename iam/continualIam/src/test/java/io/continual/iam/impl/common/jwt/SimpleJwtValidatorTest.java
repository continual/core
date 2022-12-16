package io.continual.iam.impl.common.jwt;

import org.junit.Assert;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.credentials.JwtCredential.InvalidJwtToken;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.util.time.Clock;

public class SimpleJwtValidatorTest
{
	private final String name = "name" , issuer = "issuer" , audience = "audience";
	// JWT
	private final long expTime = 1670936933000L;
	private final long offsetTime = (24 * 60 * 60);		// 1 day
	// Header - {"alg": "HS256","typ": "JWT"}
	// Payload - {"iss": "continual","sub": "continual","aud": "continual","iat": 1670936333,"exp": 1670936933}
	// Key Base64 - 12345678901234567890123456789012345678901234567890
	private final String validJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
				"eyJpc3MiOiJjb250aW51YWwiLCJzdWIiOiJjb250aW51YWwiLCJhdWQiOiJjb250aW51YWwiLCJpYXQiOjE2NzA5MzYzMzMsImV4cCI6MTY3MDkzNjkzM30." +
				"1VBwqpRd1UoRsbcabSzMROxa6xucSRghirmQMVHDYRQ";

	@Test
	public void testDefaultBuilder ()
	{
		try {
			final SimpleJwtValidator sjwtv = new SimpleJwtValidator.Builder ()
						.named ( name )
						.forIssuer ( issuer )
						.forAudience ( audience )
						.getPublicKeysFrom ( null )
						.build();
			Assert.assertNotNull ( sjwtv );
		} catch ( BuildFailure e ) {
			Assert.fail ( "Expected to create instance. " + e.getMessage() );
		}
	}

	@Test
	public void testValidate ()
	{
		try {
			Clock.useNewTestClock ().set ( expTime - offsetTime );
			final JwtCredential jwtc = new JwtCredential ( validJwtToken );
			final SimpleJwtValidator sjwtv = new SimpleJwtValidator.Builder ()
					.named ( name )
					.forIssuer ( "continual" )
					.forAudience ( "continual" )
					.getPublicKeysFrom ( null )
					.build();
			sjwtv.validate ( jwtc );
		} catch (InvalidJwtToken | BuildFailure | IamSvcException e) {
			Assert.fail ( "Expected to validate. " + e.getMessage() );
		}
	}

	@Test
	public void testValidateInvalidIssuer ()
	{
		try {
			Clock.useNewTestClock ().set ( expTime - offsetTime );
			final JwtCredential jwtc = new JwtCredential ( validJwtToken );
			final SimpleJwtValidator sjwtv = new SimpleJwtValidator.Builder ()
					.named ( name )
					.forIssuer ( issuer )
					.forAudience ( "continual" )
					.getPublicKeysFrom ( null )
					.build();
			Assert.assertFalse ( sjwtv.validate ( jwtc ) );
		} catch (InvalidJwtToken | BuildFailure | IamSvcException e) {
			Assert.fail ( "Expected to validate. " + e.getMessage() );
		}
	}

	@Test
	public void testValidateInvalidAudience ()
	{
		try {
			Clock.useNewTestClock ().set ( expTime - offsetTime );
			final JwtCredential jwtc = new JwtCredential ( validJwtToken );
			final SimpleJwtValidator sjwtv = new SimpleJwtValidator.Builder ()
					.named ( name )
					.forIssuer ( "continual" )
					.forAudience ( audience )
					.getPublicKeysFrom ( null )
					.build();
			Assert.assertFalse ( sjwtv.validate ( jwtc ) );
		} catch (InvalidJwtToken | BuildFailure | IamSvcException e) {
			Assert.fail ( "Expected to validate. " + e.getMessage() );
		}
	}

	// Inner Class Hs256SigValidator
	@Test
	public void testHs256SigValidator ()
	{
		final String secret = "12345678901234567890123456789012345678901234567890";
		try {
			final JwtCredential jwtc = new JwtCredential ( validJwtToken );
			final SimpleJwtValidator.Hs256SigValidator hsv = new SimpleJwtValidator.Hs256SigValidator ( secret );
			Assert.assertTrue ( hsv.validate ( jwtc ) );
		} catch (InvalidJwtToken e) {
			Assert.fail ( "Expected to validate. " + e.getMessage() );
		}
	}

}
