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

package io.continual.services.model.core;

import io.continual.iam.access.AccessControlList;

public class ModelOperation
{
	/**
	 * Create a new item in the given context
	 */
	public static final ModelOperation CREATE = new ModelOperation ( AccessControlList.CREATE );

	/**
	 * Read an item
	 */
	public static final ModelOperation READ = new ModelOperation ( AccessControlList.READ );

	/**
	 * Update an item
	 */
	public static final ModelOperation UPDATE = new ModelOperation ( AccessControlList.UPDATE );

	/**
	 * Delete an item
	 */
	public static final ModelOperation DELETE = new ModelOperation ( AccessControlList.DELETE );

	/**
	 * Get the string value for the selected operation.
	 * @return the value as a string
	 */
	@Override
	public String toString () { return fValue; }
	
	private ModelOperation ( String val )
	{
		fValue = val;
	}

	private final String fValue;
}
