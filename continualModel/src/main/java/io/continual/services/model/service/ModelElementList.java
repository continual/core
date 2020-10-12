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

import io.continual.util.naming.Path;

/**
 * An abstracted element list. Different modeling solutions may or may not
 * support listing children of a particular path. (For example, there may be
 * tens of thousands of entries.)  This class is used to respond to such 
 * a request with metadata about the result.
 */
public interface ModelElementList
{
	enum ResponseType
	{
		UNSUPPORTED,
		LISTING
	}

	/**
	 * Used to distinguish between a container that does not support child path
	 * listings and one that has none.
	 * @return a response type
	 */
	ResponseType getResponseType ();

	/**
	 * Elements available
	 * @return an iteration of elements
	 */
	Iterable<Path> getElements ();
}
