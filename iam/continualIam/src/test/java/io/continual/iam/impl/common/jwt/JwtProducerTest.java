package io.continual.iam.impl.common.jwt;

import org.junit.Assert;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.CommonJsonIdentity;

public class JwtProducerTest
{
	private final String issuer = "continual" , signingKey = "12345678901234567890123456789012345678901234567890";
	private final int durationSecs = 48 * 60 * 60;	// 2 days

	@Test
	public void testDefaultBuilder ()
	{
		try {
			final JwtProducer jwtp = new JwtProducer.Builder ()
					.withIssuerName ( issuer )
					.usingSigningKey ( signingKey )
					.lasting ( durationSecs )
					.build ();
			Assert.assertNotNull ( jwtp );
		} catch ( BuildFailure e ) {
			Assert.fail ( "Expected to create builder instance. " + e.getMessage() );
		}
	}

	@Test ( expected = BuildFailure.class )
	public void testBuilderException () throws BuildFailure
	{
		new JwtProducer.Builder ()
				.withIssuerName( issuer )
				.usingSigningKey ( null )
				.build();
	}

	@Test
	public void testGetValidators ()
	{
		try {
			final JwtProducer jwtp = new JwtProducer.Builder ()
					.withIssuerName ( issuer )
					.usingSigningKey ( signingKey )
					.build ();
			Assert.assertTrue ( !jwtp.getValidators ().isEmpty () );
		} catch ( BuildFailure e ) {
			Assert.fail ( "Expected to create builder instance. " + e.getMessage() );
		}
	}

	@Test
	public void testCreateJwtToken ()
	{
		try {
			final Identity id = new CommonJsonIdentity ( "user" , null , null );
			final JwtProducer jwtp = new JwtProducer.Builder ()
					.withIssuerName ( issuer )
					.usingSigningKey ( signingKey )
					.build ();
			final String result = jwtp.createJwtToken ( id, 0, null );
			Assert.assertNotNull ( result );
			Assert.assertTrue ( result.split ( "\\." ).length == 3 );
		} catch ( BuildFailure e ) {
			Assert.fail ( "Expected to create builder instance. " + e.getMessage() );
		}		
	}
}
