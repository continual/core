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
import java.util.Set;

import io.continual.iam.exceptions.IamBadRequestException;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamSvcException;

/**
 * An identity (user) in this system.
 */
public interface Identity extends UserDataHolder
{
	/**
	 * Get the unique id for this user
	 * @return a unique ID
	 */
	String getId ();

	/**
	 * Is this identity enabled?
	 * @return true if enabled
	 */
	boolean isEnabled () throws IamSvcException;

	/**
	 * Enable or disable the user. When disabled, authentication will fail.
	 * 
	 * @param enable
	 * @throws IamSvcException
	 */
	void enable ( boolean enable ) throws IamSvcException;

	/**
	 * Set the user's password.  Implementations of this interface should be careful
	 * to store the password indirectly (e.g. via salted hash), but this is not enforced
	 * at the interface layer.
	 * 
	 * @param password
	 * @throws IamSvcException
	 */
	void setPassword ( String password ) throws IamSvcException;

	/**
	 * Request a password reset. The response is a unique tag that would normally be
	 * distributed to the user via email as a link. The user acknowledges the password
	 * change request by clicking the link. The link handler then calls completePasswordReset().
	 * 
	 * @param secondsUntilExpire
	 * @param nonce
	 * @return a unique tag 
	 * @throws IamSvcException
	 * @throws IamIdentityDoesNotExist
	 * @throws IamBadRequestException
	 */
	String requestPasswordReset ( long secondsUntilExpire, String nonce ) throws IamSvcException, IamBadRequestException;

	/**
	 * Create an API key for this user.
	 * @return an API key
	 * @throws IamSvcException
	 * @throws IamSvcException
	 */
	ApiKey createApiKey () throws IamSvcException;

	/**
	 * Load the set of API keys for this user.
	 * @return a set of 0 or more API keys
	 * @throws IamSvcException
	 */
	Collection<String> loadApiKeysForUser () throws IamSvcException;

	/**
	 * Delete an API key from the user.
	 * @param key
	 * @throws IamSvcException
	 */
	void deleteApiKey ( ApiKey key ) throws IamSvcException;
	
	/**
	 * Get the IDs of the groups this user belongs to
	 * @return a set of group IDs
	 */
	Set<String> getGroupIds () throws IamSvcException;

	/**
	 * Get the groups this user belongs to
	 * @return a set of groups
	 */
	Collection<Group> getGroups () throws IamSvcException;

	/**
	 * Get a group by ID if the user is a member. Otherwise, null is returned.
	 * @param groupId
	 * @return a group or null if not a member
	 * @throws IamSvcException 
	 */
	Group getGroup ( String groupId ) throws IamSvcException;
}
