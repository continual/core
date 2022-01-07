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

import io.continual.util.naming.Path;

/**
 * A relationship between two objects within a model. The relationship is expressed as 
 * directional, with a "from" side and "to" side.<br>
 * <br>
 * Note that objects in a 1:M or M:M structure are related using multiple relation instances
 * with the same relation name.
 */
public interface ModelRelation
{
	/**
	 * Get the "from" side object path
	 * @return an object path
	 */
	Path getFrom ();

	/**
	 * Get the "to" side object path
	 * @return an object path
	 */
	Path getTo ();

	/**
	 * Get this relationship's name
	 * @return
	 */
	String getName ();

	/**
	 * Build a relation instance from two paths and a relation name.
	 * @param from
	 * @param reln
	 * @param to
	 * @return a model relation
	 */
	static ModelRelation from ( final Path from, final String reln, final Path to )
	{
		return new ModelRelation ()
		{
			@Override
			public Path getFrom () { return from; }

			@Override
			public Path getTo () { return to; }

			@Override
			public String getName () { return reln; }
		};
	}
}
