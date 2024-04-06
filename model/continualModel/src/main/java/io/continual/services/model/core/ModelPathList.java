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

import io.continual.util.naming.Path;

/**
 * A list of model paths. This list could be thousands of entries long and is therefore
 * presented only as an iterator.
 */
public interface ModelPathList extends ModelItemList<Path>
{
	/**
	 * Wrap a collection of paths with a model path list.
	 * @param paths
	 * @return a model path list
	 */
	static ModelPathList wrap ( Collection<Path> paths )
	{
		return new ModelPathList () { @Override public Iterator<Path> iterator () { return paths.iterator (); } };
	}

	/**
	 * Wrap a collection of paths with a model path list.
	 * @param paths
	 * @return a model path list
	 */
	static ModelPathList wrap ( Collection<Path> paths, PageRequest pr )
	{
		return new ModelPathList () { @Override public Iterator<Path> iterator () { return new PagingIterWrapper<> ( paths.iterator (), pr ); } };
	}

	/**
	 * Create an empty list
	 * @return an empty model path list
	 */
	static ModelPathList emptyList ()
	{
		return wrap ( new LinkedList<Path> () );
	}
}
