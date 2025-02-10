package io.continual.iam.impl.auth0;

import java.security.interfaces.RSAPublicKey;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.JwtValidator;
import io.continual.util.data.exprEval.EnvDataSource;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.exprEval.SpecialFnsDataSource;
import io.continual.util.data.json.JsonVisitor;

public class Auth0JwtValidator implements JwtValidator
{
	public Auth0JwtValidator ( JSONObject config ) throws BuildFailure
	{
		try
		{
			final ExpressionEvaluator ee = new ExpressionEvaluator ( new EnvDataSource (), new SpecialFnsDataSource () );

			fDomain = ee.evaluateText ( config.getString ( "domain" ) );
			fEmailClaimFields = new LinkedList<> ();

			final JSONArray emailClaims = config.optJSONArray ( "emailClaims" );
			if ( emailClaims != null )
			{
				fEmailClaimFields.addAll ( JsonVisitor.arrayToList ( ee.evaluateJsonArray ( emailClaims ) ) );
			}
			else
			{
				fEmailClaimFields.add ( ee.evaluateText ( config.optString ( "emailClaim" ) ) );
			}

			if ( fEmailClaimFields.size () == 0 )
			{
				throw new BuildFailure ( "No email claim fields registered." );
			}
		}
		catch ( JSONException x )
		{
			throw new BuildFailure ( x );
		}
	}

	@Override
	public boolean validate ( JwtCredential jwt ) throws IamSvcException
	{
		log.info ( "auth0: {}", jwt );
		final DecodedJWT token = validate ( jwt.toBearerString () );
		if ( token == null )
		{
			// not valid in this authenticator
			return false;
		}

		// we have a rule in Auth0 to add an email claim to the JWT
		final String userEmail = getEmailClaim ( token );
		if ( userEmail == null )
		{
			// not valid without our email
			log.warn ( "User {} was authenticated but doesn't have a recognized email claim {}.", token.getSubject() );
			return false;
		}

		return true;
	}

	@Override
	public String getSubject ( JwtCredential jwt ) throws IamSvcException
	{
		log.info ( "auth0: {}", jwt );

		final DecodedJWT token = JWT.decode ( jwt.toBearerString () );

		// we expect a rule in Auth0 to add an email claim to the JWT
		final String userEmail = getEmailClaim ( token );
		if ( userEmail == null )
		{
			// not valid without our email
			throw new IamSvcException ( "Problem extracting email claim from token." );
		}

		return userEmail;
	}

	private final String fDomain;
	private final List<String> fEmailClaimFields;

	private String getEmailClaim ( DecodedJWT token )
	{
		// we have a rule in Auth0 to add an email claim to the JWT
		for ( String emailClaimField : fEmailClaimFields )
		{
			final Claim userEmailClaim = token.getClaim ( emailClaimField );
			final String userEmail = userEmailClaim == null ? null : userEmailClaim.asString ();
			if ( userEmail != null )
			{
				return userEmail;
			}
		}
		return null;
	}
	
	private DecodedJWT validate ( String token ) throws IamSvcException
    {
		try
		{
			final DecodedJWT jwt = JWT.decode ( token );

			final JwkProvider provider = new UrlJwkProvider ( fDomain );
			final Jwk jwk = provider.get ( jwt.getKeyId () );

			final Algorithm algorithm = Algorithm.RSA256 ( (RSAPublicKey) jwk.getPublicKey (), null );

			final JWTVerifier verifier = JWT.require(algorithm)
				.withIssuer ( "https://" + fDomain + "/" )
				.build()
			;

			verifier.verify ( token );

			return jwt;
		}
		catch ( JWTVerificationException e ) 
		{
			log.info ( "token [{}...] JWTVerificationException failure: {}", token.substring ( 0, 10 ), e.getMessage () );
			return null;
		}
		catch ( JwkException e )
		{
			log.info ( "token [{}...] JwkException failure: {}", token.substring ( 0, 10 ), e.getMessage () );
			throw new IamSvcException ( e );
		}
    }

	private static final Logger log = LoggerFactory.getLogger ( Auth0JwtValidator.class );
}
