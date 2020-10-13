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

import io.continual.iam.access.ProtectedResource;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.exceptions.ModelServiceIoException;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.util.data.json.JsonSerialized;

/**
 * The model service is organized by model account. 
 */
public interface ModelAccount extends JsonSerialized, ProtectedResource
{
	/**
	 * Get the ID for this account
	 * @return the account id
	 */
	String getId ();

	/**
	 * Check if a named model exists
	 * @param mrc
	 * @param modelName
	 * @return
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	boolean doesModelExist ( ModelRequestContext mrc, String modelName ) throws ModelServiceIoException, ModelServiceRequestException;
	
	/**
	 * Get the models in a given account that are visible to the caller. If the account does not exist, an empty list is returned.
	 * @param mrc
	 * @return a list of models
	 * @throws ModelServiceIoException
	 * @throws ModelServiceRequestException
	 */
	List<String> getModelsInAccount ( ModelRequestContext mrc ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Initialize a model
	 * @param mrc
	 * @param modelName
	 * @return a model
	 * @throws ModelServiceRequestException 
	 * @throws ModelServiceIoException 
	 */
	Model initModel ( ModelRequestContext mrc, String modelName ) throws ModelServiceIoException, ModelServiceRequestException;

	/**
	 * Get information about a particular model
	 * @param mrc
	 * @param modelName
	 * @return a model
	 * @throws ModelServiceRequestException 
	 * @throws ModelServiceIoException 
	 */
	Model getModel ( ModelRequestContext mrc, String modelName ) throws ModelServiceIoException, ModelServiceRequestException;
}
