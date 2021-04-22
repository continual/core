package io.continual.iam.identity;

import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.exceptions.IamSvcException;

/**
 * An interface for validating JWOT tokens
 */
public interface JwtValidator
{
	boolean validate ( JwtCredential jwt ) throws IamSvcException;
}
