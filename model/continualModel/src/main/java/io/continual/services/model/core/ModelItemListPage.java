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

/**
 * A page of an item list. 
 */
public interface ModelItemListPage<T> extends ModelItemList<T>
{
	static final long UNKNOWN = -1L;

	/**
	 * Get the caller's page request
	 * @return the original page request
	 */
	PageRequest getPageRequest ();

	/**
	 * Can this list page report total item count?
	 * @return true if the total item count is known
	 */
	default boolean totalItemCountKnown ()
	{
		return getTotalItemCount () != UNKNOWN;
	}

	/**
	 * Get the total item count
	 * @return the total item count or UNKNOWN
	 */
	long getTotalItemCount ();

	/**
	 * Can this list page report total page count?
	 * @return true if the total page count is known
	 */
	default boolean pageCountKnown ()
	{
		return getTotalPageCount () != UNKNOWN;
	}

	/**
	 * Get the total page count 
	 * @return the total page count or UNKNOWN
	 */
	long getTotalPageCount ();

	/**
	 * Get the number of items on this page
	 * @return an item count
	 */
	long getItemCountOnPage ();

	/**
	 * Get the current page number
	 * @return the current page number
	 */
	default long getCurrentPageNumber () { return getPageRequest().getRequestedPage (); }
}
