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

import java.util.Iterator;
import java.util.LinkedList;

import io.continual.util.naming.Path;

/**
 * A paged list of model paths. This list could be thousands of entries long and is therefore
 * presented only as an iterator.
 */
public interface ModelPathListPage extends ModelItemListPage<Path>
{
	static ModelPathListPage emptyList ( PageRequest pr )
	{
		return wrap ( new LinkedList<> (), pr );
	}

	static ModelPathListPage wrap ( final LinkedList<Path> result, final PageRequest pr )
	{
		return new ModelPathListPage ()
		{
			final PagingIterWrapper<Path> iter = new PagingIterWrapper<> ( result.iterator (), pr );

			@Override
			public long getTotalItemCount () { return result.size (); }

			@Override
			public PageRequest getPageRequest () { return pr; }

			@Override
			public Iterator<Path> iterator () { return iter; }

			@Override
			public long getItemCountOnPage ()
			{
				final long pageSize = getPageRequest().getRequestedPageSize (); 
				final long pages = getTotalPageCount ();
				final long thisPage = getCurrentPageNumber ();
				final long lastPageIndex = pages - 1;

				if ( thisPage < lastPageIndex )
				{
					return pageSize;
				}
				else if ( thisPage > lastPageIndex )
				{
					return 0;
				}
				else
				{
					// last page
					return getTotalItemCount () - lastPageIndex * getPageRequest().getRequestedPageSize ();
				}
			}

			@Override
			public long getTotalPageCount ()
			{
				final long totalCount = getTotalItemCount ();
				return Math.round ( Math.ceil ( ((double) totalCount) / pr.getRequestedPageSize () ) );
			}
		};
	}

	interface PathTransform
	{
		Path transform ( Path p );
	}
	
	static ModelPathListPage wrap ( final ModelPathListPage mplp, PathTransform pt )
	{
		return new ModelPathListPage ()
		{
			@Override
			public PageRequest getPageRequest () { return mplp.getPageRequest (); }

			@Override
			public long getItemCountOnPage () { return mplp.getItemCountOnPage (); }

			@Override
			public long getTotalItemCount () { return mplp.getTotalItemCount (); }

			@Override
			public long getTotalPageCount () { return mplp.getTotalPageCount (); }

			@Override
			public Iterator<Path> iterator ()
			{
				final Iterator<Path> baseIter = mplp.iterator ();
				return new Iterator<Path> ( )
				{
					@Override
					public boolean hasNext () { return baseIter.hasNext (); }

					@Override
					public Path next () { return pt.transform ( baseIter.next () ); }
				};
			}
		};
	}
}
