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

import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;

/**
 * A database view for access queries.
 */
public interface AccessDb<G extends Group>
{
	// some common operations
	String kCreateOperation = "CREATE";
	String kReadOperation = "READ";
	String kWriteOperation = "WRITE";
	String kDeleteOperation = "DELETE";

	/**
	 * Get a group by its identifier.
	 * @param id the group's ID
	 * @return a group, or null if it does not exist
	 * @throws IamSvcException if there's a problem in the IAM service
	 */
	G loadGroup ( String id ) throws IamSvcException;

	/**
	 * load an ACL for a resource
	 * @param resource the resource for which you want the ACL
	 * @return an ACL, or null if there is none
	 * @throws IamSvcException if there's a problem in the IAM service
	 */
	AccessControlList getAclFor ( Resource resource ) throws IamSvcException;

	/**
	 * Can the given user perform the requested access?
	 * 
	 * @param id the identity/subject making the request
	 * @param resource the resource on which access is requested
	 * @param operation the operation
	 * @return true if the user is allowed to perform the operation, false otherwise
	 * @throws IamSvcException if there's a problem in the IAM service
	 */
	boolean canUser ( String id, Resource resource, String operation ) throws IamSvcException;
}
