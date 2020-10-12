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
package io.continual.iam.access;

import java.util.Set;

import io.continual.iam.exceptions.IamGroupDoesNotExist;
import io.continual.iam.exceptions.IamGroupExists;
import io.continual.iam.exceptions.IamIdentityDoesNotExist;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;

public interface AccessManager<G extends Group> extends AccessDb<G>
{
	/**
	 * Create a group
	 * @param groupDesc
	 * @return a new group with the given name
	 * @throws IamSvcException
	 */
	G createGroup ( String groupDesc ) throws IamGroupExists, IamSvcException;

	/**
	 * Create a group with a given group ID
	 * @param groupId
	 * @param groupDesc
	 * @return
	 * @throws IamSvcException
	 */
	G createGroup ( String groupId, String groupDesc ) throws IamGroupExists, IamSvcException;

	/**
	 * Add a user to a given group
	 * @param groupId
	 * @param userId
	 * @throws IamSvcException
	 * @throws IamIdentityDoesNotExist 
	 */
	void addUserToGroup ( String groupId, String userId ) throws IamSvcException, IamIdentityDoesNotExist, IamGroupDoesNotExist;

	/**
	 * Remove a user from a given group
	 * @param groupId
	 * @param userId
	 * @throws IamSvcException
	 * @throws IamIdentityDoesNotExist 
	 */
	void removeUserFromGroup ( String groupId, String userId ) throws IamSvcException, IamIdentityDoesNotExist, IamGroupDoesNotExist;

	/**
	 * Find out which groups a user is a member of.
	 * @param userId
	 * @return a set of 0 or more group IDs
	 * @throws IamSvcException
	 * @throws IamIdentityDoesNotExist 
	 */
	Set<String> getUsersGroups ( String userId ) throws IamSvcException, IamIdentityDoesNotExist;

	/**
	 * Get the set of user IDs in a particular group.
	 * @param groupId
	 * @return a set of 0 or more user IDs
	 * @throws IamSvcException
	 * @throws IamGroupDoesNotExist
	 */
	Set<String> getUsersInGroup ( String groupId ) throws IamSvcException, IamGroupDoesNotExist;
}
