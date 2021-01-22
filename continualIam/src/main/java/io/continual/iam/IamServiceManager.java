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

import io.continual.iam.access.AccessManager;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Group;
import io.continual.iam.identity.Identity;
import io.continual.iam.identity.IdentityManager;
import io.continual.iam.tags.TagManager;

/**
 * A combined interface for user/group/tag management.
 *
 */
public interface IamServiceManager<I extends Identity, G extends Group> extends IamService<I,G>
{
	/**
	 * Get the identity manager
	 * @return the identity manager
	 * @throws IamSvcException if there's a problem in the IAM service
	 */
	IdentityManager<I> getIdentityManager () throws IamSvcException;

	/**
	 * Get the access manager
	 * @return the access manager
	 * @throws IamSvcException if there's a problem in the IAM service
	 */
	AccessManager<G> getAccessManager () throws IamSvcException;

	/**
	 * Get the tag manager
	 * @return  the tag manager
	 * @throws IamSvcException if there's a problem in the IAM service
	 */
	TagManager getTagManager () throws IamSvcException;
}
