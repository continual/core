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

import java.util.Objects;

import io.continual.util.naming.Path;

/**
 * A relationship between two objects within a model. The relationship is expressed as 
 * directional, with a "from" side and "to" side.<br>
 * <br>
 * Note that objects in a 1:M or M:M structure are related using multiple relation instances
 * with the same relation name.
 */
public interface ModelRelation extends Comparable<ModelRelation>
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

			@Override
			public String toString ()
			{
				return new StringBuilder ()
					.append ( getFrom () )
					.append ( "->" )
					.append ( getName () )
					.append ( "->" )
					.append ( getTo () )
					.toString ()
				;
			}

			@Override
			public int hashCode ()
			{
				return Objects.hash ( getFrom(), getName(), getTo() );
			}

			@Override
			public boolean equals ( Object obj )
			{
				if ( this == obj ) return true;
				if ( obj == null ) return false;
				if ( getClass () != obj.getClass () ) return false;
				final ModelRelation that = (ModelRelation) obj;
				return Objects.equals ( getFrom(), that.getFrom() )
					&& Objects.equals ( getName(), that.getName() )
					&& Objects.equals ( getTo(), that.getTo() );
			}

			@Override
			public int compareTo ( ModelRelation o )
			{
				int result = from.compareTo ( o.getFrom () );
				if ( result != 0 ) return result;

				result = reln.compareTo ( o.getName () );
				if ( result != 0 ) return result;

				result = to.compareTo ( o.getTo () );
				if ( result != 0 ) return result;

				return 0;
			}
		};
	}

	static int compare ( ModelRelation mr1, ModelRelation mr2 )
	{
		int c = mr1.getFrom ().compareTo ( mr2.getFrom () );
		if ( c != 0 ) return c;

		c = mr1.getName ().compareTo ( mr2.getName () );
		if ( c != 0 ) return c;

		return mr1.getTo ().compareTo ( mr2.getTo () );
	}
}

