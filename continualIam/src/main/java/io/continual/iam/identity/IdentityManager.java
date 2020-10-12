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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamIdentityExists;
import io.continual.iam.exceptions.IamSvcException;

/**
 * This interface to the database allows the caller to make changes to identity
 * information, like creating users, enabling/disabling users, etc.<br>
 * <br>
 * A userId string can be any value suitable to the application, such as email
 * address or UUID. Note that userIds are fixed, so using an email address comes
 * with some risk of identity migration work if the email address changes.
 */
public interface IdentityManager<I extends Identity> extends IdentityDb<I>
{
	/**
	 * Find out if a given user exists.
	 * @param userId
	 * @return true if the user exists in the identity manager.
	 * @throws IamSvcException
	 */
	boolean userExists ( String userId ) throws IamSvcException;

	/**
	 * Find out if a given user or alias exists.
	 * @param userIdOrAlias
	 * @return true if the user exists by userId or alias in the identity manager.
	 * @throws IamSvcException
	 */
	boolean userOrAliasExists ( String userIdOrAlias ) throws IamSvcException;

	/**
	 * Load a user from the identity manager. 
	 * @param userId 
	 * @return a user or null if the user doesn't exist
	 * @throws IamSvcException
	 */
	I loadUser ( String userId ) throws IamSvcException;

	/**
	 * Load a user from the identity manager. 
	 * @param userIdOrAlias the actual userId or an alias
	 * @return a user or null if the user doesn't exist
	 * @throws IamSvcException
	 */
	I loadUserOrAlias ( String userIdOrAlias ) throws IamSvcException;

	/**
	 * Find users with a user ID that starts with the given string
	 * @param startingWith
	 * @return a list of 0 or more matching user IDs
	 * @throws IamSvcException
	 */
	List<String> findUsers ( String startingWith ) throws IamSvcException;

	/**
	 * Create a new user in the identity manager. The username for this user
	 * defaults to the userId value provided here.
	 * 
	 * @param userId
	 * @return the new user
	 * @throws IamIdentityExists if the user exists
	 * @throws IamSvcException
	 */
	I createUser ( String userId ) throws IamIdentityExists, IamSvcException;

	/**
	 * Create a new anonymous user in the identity manager. 
	 * @return a new anonymous user 
	 * @throws IamSvcException
	 */
	I createAnonymousUser () throws IamSvcException;

	/**
	 * Delete a user from the identity manager.
	 * @param userId
	 * @throws IamSvcException
	 */
	void deleteUser ( String userId ) throws IamSvcException;

	/**
	 * Add a username/alias for this user. Identity DBs should normally support
	 * multiple aliases (username, email, mobile phone, etc.). Tracking them
	 * beyond being references to an identity record is done at the application level.
	 * 
	 * @param userId
	 * @param alias
	 * @throws IamSvcException
	 * @throws IamBadRequestException 
	 */
	void addAlias ( String userId, String alias ) throws IamSvcException, IamBadRequestException;
	
	/**
	 * Remove a username/alias from the database. A userId may not be removed
	 * (disable the user instead).
	 * @param alias
	 * @throws IamBadRequestException If a userId is provided.
	 * @throws IamSvcException
	 */
	void removeAlias ( String alias ) throws IamBadRequestException, IamSvcException;

	/**
	 * Get the aliases for a userId. The result must be non-null but may be empty. The userId
	 * is not included in the list.
	 * @param userId
	 * @return a collection of 0 or more aliases for a userId
	 * @throws IamSvcException
	 * @throws IamIdentityDoesNotExist 
	 */
	Collection<String> getAliasesFor ( String userId ) throws IamSvcException, IamIdentityDoesNotExist;
	
	/**
	 * Complete a password reset by providing a tag and a new password. The update
	 * will fail if the tag is unknown or has expired. See
	 * {@link Identity#requestPasswordReset(long, String) requestPasswordReset}
	 * for details on creating a password reset tag.
	 * 
	 * @param tag
	 * @param newPassword
	 * @return true if the password was updated successfully.
	 * @throws IamSvcException
	 */
	boolean completePasswordReset ( String tag, String newPassword ) throws IamSvcException;

	/**
	 * Load an API key record based on the API key ID.
	 * @param apiKey
	 * @return an API key or null if it doesn't exist
	 * @throws IamSvcException
	 */
	ApiKey loadApiKeyRecord ( String apiKey ) throws IamSvcException;

	/**
	 * Get all user IDs in this db. Clearly not suitable for systems beyond a few thousand
	 * users. For larger scale, this call may throw an IamSvcException signaling that
	 * the underlying database won't return a user list. 
	 * @return a collection of user Ids
	 * @throws IamSvcException
	 */
	Collection<String> getAllUsers () throws IamSvcException;
	
	/**
	 * Load all users in this identity manager. Clearly not suitable for systems beyond a
	 * few thousand users. For larger scale, this call may throw an IamSvcException
	 * signaling that the underlying database won't return a user list.
	 */
	Map<String,I> loadAllUsers () throws IamSvcException;
}
