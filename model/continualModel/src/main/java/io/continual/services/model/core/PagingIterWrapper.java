package io.continual.services.model.core;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A simple wrapper for paging through an iterator.  Note that iteration ends when 
 * the original iteration is out of data or a complete page has been delivered.
 * @param <T>
 */
public class PagingIterWrapper<T> implements Iterator<T>
{
	public PagingIterWrapper ( Iterator<T> iterator, PageRequest pr )
	{
		fIter = iterator;
		fSkipsLeft = pr.getRequestedPage () * pr.getRequestedPageSize ();
		fItemsLeft = pr.getRequestedPageSize ();
	}

	@Override
	public boolean hasNext ()
	{
		initialPageSkip ();
		return fItemsLeft > 0 && fIter.hasNext ();
	}

	@Override
	public T next ()
	{
		if ( !hasNext () )
		{
			throw new NoSuchElementException ();
		}

		initialPageSkip ();
		fItemsLeft--;
		return fIter.next ();
	}

	private final Iterator<T> fIter;
	private int fSkipsLeft;
	private int fItemsLeft;

	private void initialPageSkip ()
	{
		while ( fSkipsLeft > 0 && fIter.hasNext () )
		{
			fSkipsLeft--;
			fIter.next ();	// discard
		}
	}
}
