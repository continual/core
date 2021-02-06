package io.continual.basesvcs.services.accounts;

import io.continual.iam.access.AccessManager;
import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.credentials.JwtCredential.InvalidJwtToken;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.IdentityManager;
import io.continual.iam.tags.TagManager;
import io.continual.services.Service;
import io.continual.util.naming.Path;

public interface AccountService<I extends Identity,G extends Group>
	extends Service, IdentityManager<I>, AccessManager<G>, TagManager
{
	public static final String kSysAdminGroup = "systemAdmin";

	/**
	 * Get the base path for the given user.
	 * @param user
	 * @return a path
	 * @throws IamSvcException 
	 * @throws ModelItemDoesNotExistException 
	 */
	Path getAccountBasePath ( Identity user ) throws IamSvcException, AccountItemDoesNotExistException;

	/**
	 * Setup the standard base path for the given user
	 * @param user
	 * @return a path
	 * @throws IamSvcException
	 * @throws ModelItemDoesNotExistException
	 */
	Path setStandardAccountBasePath ( Identity user ) throws IamSvcException, AccountItemDoesNotExistException;

	/**
	 * Create a JWT token for the given identity
	 * @param ii
	 * @return a token
	 * @throws IamSvcException 
	 */
	String createJwtToken ( Identity ii ) throws IamSvcException;

	/**
	 * Parse a JWT token
	 * @param token
	 * @return a JWT credential which can be trusted as valid
	 * @throws InvalidJwtToken
	 * @throws IamSvcException
	 */
	JwtCredential parseJwtToken ( String token ) throws InvalidJwtToken, IamSvcException;

	/**
	 * Invalidate the given JWT token, preventing further authentication with it.
	 * @param string a JWT credential
	 * @throws IamSvcException 
	 */
	void invalidateJwtToken ( String token ) throws IamSvcException;
}
