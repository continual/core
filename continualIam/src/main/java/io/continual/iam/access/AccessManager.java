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

import java.util.Collection;
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
	 * @param groupDesc the group description 
	 * @return a new group with the given description
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 */
	G createGroup ( String groupDesc ) throws IamGroupExists, IamSvcException;

	/**
	 * Create a group with a given group ID
	 * @param groupId a group ID
	 * @param groupDesc a group description
	 * @return a new group with the given id and description
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 */
	G createGroup ( String groupId, String groupDesc ) throws IamGroupExists, IamSvcException;

	/**
	 * Add a user to a given group
	 * @param groupId a group ID
	 * @param userId a user ID
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 * @throws IamIdentityDoesNotExist when the identity doesn't exist
	 */
	void addUserToGroup ( String groupId, String userId ) throws IamSvcException, IamIdentityDoesNotExist, IamGroupDoesNotExist;

	/**
	 * Remove a user from a given group
	 * @param groupId a group ID
	 * @param userId a user ID
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 * @throws IamIdentityDoesNotExist when the identity doesn't exist
	 */
	void removeUserFromGroup ( String groupId, String userId ) throws IamSvcException, IamIdentityDoesNotExist, IamGroupDoesNotExist;

	/**
	 * Find out which groups a user is a member of.
	 * @param userId a user ID
	 * @return a set of 0 or more group IDs
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 * @throws IamIdentityDoesNotExist when the identity doesn't exist 
	 */
	Set<String> getUsersGroups ( String userId ) throws IamSvcException, IamIdentityDoesNotExist;

	/**
	 * Get the set of user IDs in a particular group.
	 * @param groupId a group ID
	 * @return a set of 0 or more user IDs
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 * @throws IamGroupDoesNotExist when the identity doesn't exist
	 */
	Set<String> getUsersInGroup ( String groupId ) throws IamSvcException, IamGroupDoesNotExist;

	/**
	 * Get all group IDs in this db. Clearly not suitable for systems beyond a few thousand
	 * groups. For larger scale, this call may throw an IamSvcException signaling that
	 * the underlying database won't return a group list. 
	 * @return a collection of group Ids
	 * @throws IamSvcException when the call cannot be completed due to a service error
	 */
	Collection<String> getAllGroups () throws IamSvcException;
}
