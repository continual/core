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

import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

/**
 * A full path, including account and model name, to a model object.
 */
public class ModelObjectPath
{
	public ModelObjectPath ( String acctId, String modelName, Path objectPath )
	{
		fAcctId = acctId;
		fModelName = modelName;
		fObjectPath = objectPath;
	}

	/**
	 * Get the account ID for this object
	 * @return the account ID
	 */
	public String getAcctId () { return fAcctId; }

	/**
	 * Get the model name for this object in the account
	 * @return the model name
	 */
	public String getModelName () { return fModelName; }

	/**
	 * Get the path to this object within the model.
	 * @return the object path
	 */
	public Path getObjectPath () { return fObjectPath; }

	/**
	 * Get the unique ID for this object path.
	 * @return a unique ID
	 */
	public String getId ()
	{
		final Path modelPart = Path.getRootPath ()
			.makeChildItem ( Name.fromString ( fAcctId ) )
			.makeChildItem ( Name.fromString ( fModelName ) )
			.makeChildPath ( fObjectPath )
		;
		return modelPart.getId ();
	}

	/**
	 * Get the path to a child item of this item
	 * @param itemName
	 * @return a child item
	 */
	public ModelObjectPath makeChildItem ( Name itemName )
	{
		final Path myPath = getObjectPath ();
		if ( myPath == null || myPath.equals ( Path.getRootPath () ) )
		{
			return new ModelObjectPath ( getAcctId(), getModelName(), Path.getRootPath ().makeChildItem ( itemName ) );
		}
		else
		{
			return new ModelObjectPath ( getAcctId(), getModelName(), getObjectPath().makeChildItem ( itemName ) );
		}
	}

	/**
	 * Get the path to a parent object.
	 * @return
	 */
	public ModelObjectPath getParentPath ()
	{
		final Path parent = getObjectPath().getParentPath ();
		if ( parent == null ) return null;
		return new ModelObjectPath ( getAcctId(), getModelName(), parent );
	}

	@Override
	public String toString ()
	{
		return getId ();
	}
	
	private final String fAcctId;
	private final String fModelName;
	private final Path fObjectPath;

}
