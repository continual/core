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
package io.continual.iam;

import io.continual.iam.access.AccessDb;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.IdentityDb;

/**
 * Identity and access lookup interface, planned for "lookups" rather than
 * the management of identity, groups, access, etc.
 *
 */
public interface IamService<I extends Identity, G extends Group>
{
	/**
	 * Get the identity database
	 * @return the identity database
	 * @throws IamSvcException if there's a problem in the IAM service
	 */
	IdentityDb<I> getIdentityDb () throws IamSvcException;

	/**
	 * Get the access database
	 * @return the access database
	 * @throws IamSvcException if there's a problem in the IAM service
	 */
	AccessDb<G> getAccessDb () throws IamSvcException;
}
