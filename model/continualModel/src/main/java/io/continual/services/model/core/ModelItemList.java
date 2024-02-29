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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A list of model instances. This list could be thousands of entries long and is therefore
 * presented only as an iterator.
 */
public interface ModelItemList<T> extends Iterable<T>
{
	/**
	 * Filter this list with the given filter
	 * @param mof
	 * @return a filtered list
	 */
	default ModelItemList<T> filter ( ModelItemFilter<T> mof )
	{
		final LinkedList<T> result = new LinkedList<> ();

		final Iterator<T> it = this.iterator ();
		while ( it.hasNext () )
		{
			final T mo = it.next ();
			if ( mof.matches ( mo ) )
			{
				result.add ( mo );
			}
		}

		return new ModelItemList<T> ()
		{
			@Override
			public Iterator<T> iterator ()
			{
				return result.iterator ();
			}
		};
	}

	/**
	 * Convenience method for creating a small list of items.
	 * @return a list of objects
	 */
	static <T> ModelItemList<T> simpleListOfCollection ( Collection<T> instances )
	{
		return new ModelItemList<T> ()
		{
			@Override
			public Iterator<T> iterator ()
			{
				return instances.iterator ();
			}
		};
	}

	/**
	 * Convenience method for iterating into a list
	 * @param list
	 * @return a list of items
	 */
	static <T> List<T> iterateIntoList ( ModelItemList<T> list )
	{
		final LinkedList<T> result = new LinkedList<> ();
		final Iterator<T> it = list.iterator ();
		while ( it.hasNext () )
		{
			result.add ( it.next () );
		}
		return result;
	}
}
