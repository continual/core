/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package io.continual.iam.identity;

import io.continual.iam.credentials.ApiKeyCredential;
import io.continual.iam.credentials.JwtCredential;
import io.continual.iam.credentials.UsernamePasswordCredential;
import io.continual.iam.exceptions.IamSvcException;

/**
 * An identity database, mainly for authenticating users.
 */
public interface IdentityDb<I extends Identity>
{
	/**
	 * Find out if a given user exists.
	 * @param userId a user ID
	 * @return true if the user exists in the identity manager.
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 */
	boolean userExists ( String userId ) throws IamSvcException;

	/**
	 * Find out if a given user or alias exists.
	 * @param userIdOrAlias the user ID or an alias
	 * @return true if the user exists by userId or alias in the identity manager.
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 */
	boolean userOrAliasExists ( String userIdOrAlias ) throws IamSvcException;

	/**
	 * Load a user from the identity manager. 
	 * @param userId a user ID
	 * @return a user or null if the user doesn't exist
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 */
	I loadUser ( String userId ) throws IamSvcException;

	/**
	 * Load a user from the identity manager. 
	 * @param userIdOrAlias the actual userId or an alias
	 * @return a user or null if the user doesn't exist
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 */
	I loadUserOrAlias ( String userIdOrAlias ) throws IamSvcException;

	/**
	 * Authenticate with a username and password
	 * @param upc the username/password credential
	 * @return an authenticated identity or null
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 */
	I authenticate ( UsernamePasswordCredential upc ) throws IamSvcException;

	/**
	 * Authenticate with an API key and signature
	 * @param akc the API key credential
	 * @return an authenticated identity or null
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 */
	I authenticate ( ApiKeyCredential akc ) throws IamSvcException;

	/**
	 * Authenticate with a JWT token
	 * @param jwt the JWT credential
	 * @return an authenticated identity or null
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 */
	I authenticate ( JwtCredential jwt ) throws IamSvcException;

	/**
	 * Create a JWT token for the given identity.
	 * @param ii an identity
	 * @return a JWT token
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 */
	String createJwtToken ( Identity ii ) throws IamSvcException;

	/**
	 * Invalidate the given JWT token
	 * @param jwtToken a JWT token
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 */
	void invalidateJwtToken ( String jwtToken ) throws IamSvcException;
}
