package io.continual.services.model.core;

public class PageRequest
{
	public PageRequest ()
	{
	}

	public PageRequest startingAtPage ( int zeroBasedPageIndex )
	{
		if ( zeroBasedPageIndex < 0 )
		{
			throw new IllegalArgumentException ( "page index must be at least 0" );
		}
		fRequestedPage = zeroBasedPageIndex;
		return this;
	}
	
	public PageRequest withPageSize ( int pageSize )
	{
		if ( pageSize < 1 )
		{
			throw new IllegalArgumentException ( "page size must be at least 1" );
		}
		fPageSize = pageSize;
		return this;
	}

	public int getRequestedPage ()
	{
		return fRequestedPage;
	}

	public int getRequestedPageSize ()
	{
		return fPageSize;
	}

	private int fRequestedPage = 0;
	private int fPageSize = Integer.MAX_VALUE; 
}
