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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A list of model objects. This list could be thousands of entries long and is therefore
 * presented only as an iterator.
 */
public interface ModelObjectList<T> extends ModelItemList<ModelObjectAndPath<T>>
{
	/**
	 * Construct an empty object list
	 * @return an empty list
	 */
	static ModelObjectList<?> emptyList ()
	{
		return simpleList ();
	}

	/**
	 * Create an empty object list of the given type
	 * @param <T>
	 * @param clazz
	 * @return an empty list
	 */
	static <T> ModelObjectList<T> emptyList ( Class<T> clazz )
	{
		final List<ModelObjectAndPath<T>> list = new LinkedList<> ();
		return new ModelObjectList<T> ()
		{
			@Override
			public Iterator<ModelObjectAndPath<T>> iterator ()
			{
				return list.iterator ();
			}
		};
	}

	/**
	 * Convenience method for creating a small list of objects.
	 * @return a list of objects
	 */
	@SafeVarargs
	static <T> ModelObjectList<T> simpleList ( ModelObjectAndPath<T>... instances )
	{
		final List<ModelObjectAndPath<T>> list = Arrays.asList ( instances );
		return new ModelObjectList<T> ()
		{
			@Override
			public Iterator<ModelObjectAndPath<T>> iterator ()
			{
				return list.iterator ();
			}
		};
	}

	/**
	 * Convenience method for creating a list from a collection
	 * @param instances
	 * @return a list of objects
	 */
	static <T> ModelObjectList<T> simpleListOfCollection ( Collection<ModelObjectAndPath<T>> instances )
	{
		return new ModelObjectList<T> ()
		{
			@Override
			public Iterator<ModelObjectAndPath<T>> iterator ()
			{
				return instances.iterator ();
			}
		};
	}
}
