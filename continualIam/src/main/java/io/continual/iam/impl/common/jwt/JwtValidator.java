package io.continual.iam.impl.common.jwt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.util.data.Sha256HmacSigner;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.data.json.JsonVisitor.ObjectVisitor;

public class JwtValidator
{
	public static class Builder
	{
		public JwtValidator build () throws BuildFailure { return new JwtValidator ( this ); }

		public Builder named ( String name ) { fName = name; return this; }
		public Builder forIssuer ( String iss ) { fIssuers.add ( iss ); return this; }
		public Builder forAudience ( String aud ) { fAudience = aud; return this; }
		public Builder getPublicKeysFrom ( String pkurl ) { fPublicKeyUrl = pkurl; return this; }

		private String fName = "(anonymous)";
		private String fAudience = "";
		private final TreeSet<String> fIssuers = new TreeSet<> ();
		private String fPublicKeyUrl = null;
	}

	public boolean validate ( JwtCredential jwt ) throws IamSvcException
	{
		// does the issuer match the one this validator reads for?
		final String iss = jwt.getIssuer ();
		if ( !fIssuers.contains ( iss ) )
		{
			log.info ( "The JWT is not from an issuer the {} validator recognizes.", fName );
			return false;
		}

		if ( !jwt.isForAudience ( fAudience ) )
		{
			log.info ( "The JWT is not for the {} validator's audience.", fName );
			return false;
		}

		final List<SigValidator> validators = getValidators ();
		for ( SigValidator v : validators )
		{
			if ( v.validate ( jwt ) ) return true;
		}
		return false;
	}

	public List<SigValidator> getValidators ()
	{
		final LinkedList<SigValidator> result = new LinkedList<> ();
		result.addAll ( fSigValidators );
		return result;
	}

	private final String fName;
	private final String fAudience;
	private final TreeSet<String> fIssuers = new TreeSet<> ();
	private final LinkedList<SigValidator> fSigValidators = new LinkedList<> ();

	protected JwtValidator ( Builder b ) throws BuildFailure
	{
		fName = b.fName;
		fIssuers.addAll ( b.fIssuers );
		fAudience = b.fAudience;

		if ( b.fPublicKeyUrl != null )
		{
			fSigValidators.addAll ( readJwk ( b.fPublicKeyUrl ) );
		}
		
		if ( fIssuers.size () < 1 ) throw new BuildFailure ( "No issuers specified for validator." );
		if ( fAudience == null || fAudience.length () == 0 ) throw new BuildFailure ( "No audience specified for validator." );
	}

	protected interface SigValidator
	{
		boolean validate ( JwtCredential jwt );
	}

	protected static class Hs256SigValidator implements SigValidator
	{
		public Hs256SigValidator ( String secret )
		{
			fSecret = secret;
		}

		@Override
		public boolean validate ( JwtCredential jwt )
		{
			final String signedPart = jwt.getSignedContent ();
			final String signature = jwt.getSignature ();

			final byte[] expectedSigBytes = Sha256HmacSigner.signToBytes ( signedPart, fSecret );
			final String expectedSig = TypeConvertor.base64UrlEncode ( expectedSigBytes );
			if ( expectedSig.equals ( signature ) )
			{
				return true;
			}
			return false;
		}
		
		private final String fSecret;
	}
	
	private static BigInteger stringToInt ( String exponent )
	{
		final byte[] unsignedBigEndianOctetSeq = TypeConvertor.base64UrlDecode ( exponent );

//		final byte[] moreBytes = new byte[len+1];
//		System.arraycopy ( bytes, 0, moreBytes, 1, len );
//		final BigInteger bi = new BigInteger ( moreBytes );

		final BigInteger bi = new BigInteger ( 1, unsignedBigEndianOctetSeq );

		return bi;
	}

	protected static class RsaValidator implements SigValidator
	{
		public RsaValidator ( JSONObject keyEntry ) throws NoSuchAlgorithmException, InvalidKeySpecException
		{
			final BigInteger exponentBi = stringToInt ( keyEntry.getString ( "e" ) );
			final BigInteger modBi = stringToInt ( keyEntry.getString ( "n" ) );

			final RSAPublicKeySpec fKeySpec = new RSAPublicKeySpec ( modBi, exponentBi );
			final KeyFactory keyFactory = KeyFactory.getInstance ( "RSA" );
			fPubKey = keyFactory.generatePublic ( fKeySpec );

			final String base64Enc = new String ( Base64.getEncoder().encodeToString ( fPubKey.getEncoded () ) );
			log.info ( "key is: {}", base64Enc );
		}

		public RsaValidator ( String pem ) throws CertificateException
		{
			final CertificateFactory fact = CertificateFactory.getInstance ( "X.509" );
			final X509Certificate cer = (X509Certificate) fact.generateCertificate ( new ByteArrayInputStream ( pem.getBytes ( StandardCharsets.UTF_8 ) ) );

			fPubKey = cer.getPublicKey ();
		}

		@Override
		public boolean validate ( JwtCredential jwt )
		{
			try
			{
				final String signedPart = jwt.getSignedContent ();

				final String signature = jwt.getSignature ();
				final byte[] jwtSigBytes = TypeConvertor.base64UrlDecode ( signature );

				final Signature sig = Signature.getInstance ( "SHA256withRSA" );
				sig.initVerify ( fPubKey );
				sig.update ( signedPart.getBytes ( StandardCharsets.UTF_8 ) );
				final boolean verified = sig.verify ( jwtSigBytes );
				return verified;
			}
			catch ( GeneralSecurityException e )
			{
				log.warn ( "Unable to produce RSA with SHA-256 signature check.", e );
				return false;
			}
		}

		private final PublicKey fPubKey;
	}

	private static List<SigValidator> readJwk ( String pkUrl ) throws BuildFailure
	{
		log.info ( "Reading keys from {}", pkUrl );

		final LinkedList<SigValidator> result = new LinkedList<> ();
		try
		{
			final URL url = new URL ( pkUrl );
			final URLConnection request = url.openConnection ();
			request.connect ();
			try ( final InputStream is = (InputStream) request.getContent () )
			{
				final JSONObject o = new JSONObject ( new CommentedJsonTokener ( is ) );
				
				if ( o.has ( "keys" ) )
				{
					JsonVisitor.forEachElement ( o.getJSONArray ( "keys" ), new ArrayVisitor<JSONObject,GeneralSecurityException> ()
					{
						@Override
						public boolean visit ( JSONObject keyEntry ) throws JSONException, GeneralSecurityException
						{
							if ( keyEntry.getString ( "kty" ).equals ( "RSA" ) )
							{
								result.add ( new RsaValidator ( keyEntry ) );
							}
	//						else if ( keyEntry.getString ( "alg" ).equals ( "HS256" ) )
	//						{
	//							result.add ( new Hs256SigValidator ( keyString.getString ( "" ) ) );
	//						}
							return true;
						}
					});
				}
				else
				{
					// PEM keys
					JsonVisitor.forEachElement ( o, new ObjectVisitor<String,CertificateException> ()
					{
						@Override
						public boolean visit ( String key, String pem ) throws CertificateException
						{
							result.add ( new RsaValidator ( pem ) );
							return true;
						}
					} );
				}
			}
			catch ( GeneralSecurityException x )
			{
				throw new BuildFailure ( x );
			}

			return result;
		}
		catch ( JSONException | IOException e )
		{
			throw new BuildFailure ( e );
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( JwtValidator.class );
}
