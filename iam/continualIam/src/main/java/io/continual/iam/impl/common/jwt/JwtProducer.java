package io.continual.iam.impl.common.jwt;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.identity.Identity;
import io.continual.util.data.Sha256HmacSigner;
import io.continual.util.data.TypeConvertor;
import io.continual.util.time.Clock;

public class JwtProducer extends SimpleJwtValidator
{
	public static class Builder extends io.continual.iam.impl.common.jwt.SimpleJwtValidator.Builder
	{
		@Override
		public JwtProducer build () throws BuildFailure { return new JwtProducer ( this ); }

		public Builder withIssuerName ( String i )
		{
			fIssuer = i;
			super.forIssuer ( i );
			super.forAudience ( i );
			return this;
		}

		public Builder usingSigningKey ( String k ) { fSigningKey = k; return this; }
		public Builder lasting ( int seconds ) { fDurationSecs = seconds; return this; }

		private String fIssuer;
		private String fSigningKey;
		private int fDurationSecs = 24 * 60 * 60;
	}

	/**
	 * Create a JWT token for the given identity. The audience is this issuer.
	 * @param ii an identity for which to create the JWT
	 * @param duration use &lt; 0 for default
	 * @param tu time unit of duration
	 * @return a JWT token string
	 */
	public String createJwtToken ( Identity ii, long duration, TimeUnit tu )
	{
		// header
		final JSONObject header = new JSONObject ()
			.put ( "alg", "HS256" )
			.put ( "typ", "JWT" )
		;
		final String encodedHeader = TypeConvertor.base64UrlEncode ( header.toString () ); 

		final long durationToUseMs = duration > 0 && tu != null ?
			TimeUnit.MILLISECONDS.convert ( duration, tu ) :
			fDurationSecs * 1000L
		;

		// payload
		final JSONObject payload = new JSONObject ()
			.put ( "iss", fIssuer )
			.put ( "sub", ii.getId () )
			.put ( "aud", new JSONArray ().put ( fIssuer ) )
			.put ( "exp", ( Clock.now () + durationToUseMs ) / 1000L )
		;
		final String encodedPayload = TypeConvertor.base64UrlEncode ( payload.toString () ); 

		final String headerAndPayload = encodedHeader + "." + encodedPayload;
		final byte[] sigBytes = Sha256HmacSigner.signToBytes ( headerAndPayload, fSigningKey );
		final String signature = TypeConvertor.base64UrlEncode ( sigBytes );

		return headerAndPayload + "." + signature;
	}

	public List<SigValidator> getValidators ()
	{
		final List<SigValidator> result = super.getValidators ();
		result.add ( new Hs256SigValidator ( fSigningKey ) );
		return result;
	}

	protected JwtProducer ( Builder b ) throws BuildFailure
	{
		super ( b );

		fIssuer = b.fIssuer;
		fSigningKey = b.fSigningKey;
		fDurationSecs = b.fDurationSecs;

		if ( fIssuer == null || fSigningKey == null )
		{
			// we're not configured to issue JWT tokens
			throw new BuildFailure ( "An issuer and a key are required to produce JWT tokens." );
		}
	}

//	protected abstract void storeInvalidJwtToken ( String token ) throws IamSvcException;
//	protected abstract boolean isInvalidJwtToken ( String token ) throws IamSvcException;

	private final String fIssuer;
	private final String fSigningKey;
	private final int fDurationSecs;
}
