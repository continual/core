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

/**
 * An operation over a model or model object.
 */
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
	 * Update an ACL
	 */
	public static final ModelOperation ACL_UPDATE = new ModelOperation ( "acl_update" );

	/**
	 * The array of all operations
	 */
	public static ModelOperation[] kAllOperations = new ModelOperation[]
	{
		CREATE,
		READ,
		UPDATE,
		DELETE,
		ACL_UPDATE
	};
	
	/**
	 * The set of all operations as strings
	 */
	public static String[] kAllOperationStrings = new String[]
	{
		CREATE.toString (),
		READ.toString (),
		UPDATE.toString (),
		DELETE.toString (),
		ACL_UPDATE.toString ()
	};

	/**
	 * Get the string value for the selected operation.
	 * @return the value as a string
	 */
	@Override
	public String toString ()
	{
		return fValue;
	}
	
	@Override
	public int hashCode ()
	{
		return fValue.hashCode ();
	}

	@Override
	public boolean equals ( Object obj )
	{
		if ( this == obj ) return true;
		if ( obj == null ) return false;
		if ( getClass () != obj.getClass () ) return false;
		return fValue.equals ( ((ModelOperation) obj).fValue );
	}

	private ModelOperation ( String val )
	{
		fValue = val;
		if ( fValue == null ) throw new IllegalArgumentException ( "A model operation cannot have a null value." );
	}

	private final String fValue;
}
