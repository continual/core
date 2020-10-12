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

package io.continual.services.model.service;

import java.util.List;

import io.continual.iam.identity.Identity;
import io.continual.services.Service;

import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelRequestContext.CacheControl;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;

/**
 * The model service, which is a global view over all models everywhere.
 * By "all models" we really mean all of them, including models that belong
 * to other users or are shared data.
 */
public interface ModelService extends Service
{
	/**
	 * Build a request context
	 */
	interface RequestContextBuilder
	{
		/**
		 * The request is for the given user
		 * @param forWhom
		 * @return this request context builder
		 */
		RequestContextBuilder forUser ( Identity forWhom );

		/**
		 * Set how the request should use the cache
		 * @param caching
		 * @return this reuqest context builder
		 */
		RequestContextBuilder usingCache ( CacheControl caching );

		/**
		 * Build the request context
		 * @return a request context
		 */
		ModelRequestContext build ();
	}

	/**
	 * Get the limits and capabilities list for this model service.
	 * @return a model limit/cap entry
	 */
	ModelLimitsAndCaps getLimitsAndCaps ();

	/**
	 * Build a request context
	 * @return a request context builder
	 */
	RequestContextBuilder buildRequestContext ();

	/**
	 * Create an account if it doesn't yet exist
	 * @param mrc
	 * @param acctId
	 * @param ownerId
	 * @return a record of the new account, or null if the account was not created
	 * @throws ModelServiceRequestException 
	 * @throws ModelServiceIoException 
	 */
	ModelAccount createAccount ( ModelRequestContext mrc, String acctId, String ownerId ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Get a list of existing accounts. (Not an approach viable for a long-term successful system!)
	 * @param mrc
	 * @return a list of acct IDs
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	List<String> getAccounts ( ModelRequestContext mrc ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Get details for a given account
	 * @param mrc
	 * @param acctId
	 * @return an account object
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	ModelAccount getAccount ( ModelRequestContext mrc, String acctId ) throws ModelServiceIoException, ModelServiceRequestException;
}
