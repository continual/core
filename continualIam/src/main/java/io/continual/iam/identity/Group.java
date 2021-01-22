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

import java.util.Set;

import io.continual.iam.exceptions.IamSvcException;

/**
 * A group of users
 */
public interface Group extends UserDataHolder
{
	/**
	 * Get an identifier for this group
	 * @return a string id
	 */
	String getId ();

	/**
	 * Get a readable name for this group
	 * @return a name that's not necessarily unique in the system
	 */
	String getName ();

	/**
	 * Is the given user a member of this group?
	 * @param userId a user ID
	 * @return true if the user is a member
	 * @throws IamSvcException if there's a problem in the IAM service 
	 */
	boolean isMember ( String userId ) throws IamSvcException;

	/**
	 * Get members of the group
	 * @return a set of 0 or more user IDs
	 * @throws IamSvcException if there's a problem in the IAM service 
	 */
	Set<String> getMembers () throws IamSvcException;
}
