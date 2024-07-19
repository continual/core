package io.continual.iam.identity;

import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.exceptions.IamSvcException;

/**
 * An interface for validating JWOT tokens
 */
public interface JwtValidator
{
	/**
	 * Validate the given JWT token
	 * @param jwt
	 * @return true if the token is valid
	 * @throws IamSvcException
	 */
	boolean validate ( JwtCredential jwt ) throws IamSvcException;

	/**
	 * Get the subject of the JWT (for systems in which the subject is encoded in
	 * a claim)
	 * @param jwt
	 * @return the subject string
	 */
	default String getSubject ( JwtCredential jwt ) throws IamSvcException
	{
		return jwt.getSubject ();
	}
}
