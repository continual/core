package io.continual.util.collections;

import org.junit.Test;

import io.continual.util.collections.ShardedExpiringCache.Fetcher;
import io.continual.util.collections.ShardedExpiringCache.Fetcher.FetchException;

public class ExceptionTest
{
	@Test(expected = FetchException.class)
	public void testRead1 () throws FetchException
	{
		final ShardedExpiringCache<String,String> cacheShard = new ShardedExpiringCache.Builder<String,String> ()
				.withShardCount ( 1024 )
				.build ()
			;
		cacheShard.read( "FetchExceptionMsg" , null , new TestFetchException () );
	}

	@Test(expected = FetchException.class)
	public void testRead2 () throws FetchException
	{
		final ShardedExpiringCache<String,String> cacheShard = new ShardedExpiringCache.Builder<String,String> ()
				.withShardCount ( 1024 )
				.build ()
			;
		cacheShard.read( "FetchExceptionThrw" , null , new TestFetchException () );
	}

	private static class TestFetchException implements Fetcher<String,String>
	{
		@Override
		public String fetch ( String key ) throws FetchException
		{
			if ( "FetchExceptionMsg".equals(key) )
			{
				throw new FetchException( "Test Fetch Exception Message" );
			}
			else if ( "FetchExceptionThrw".equals(key) )
			{
				throw new FetchException( new Throwable ( "Test Fetch Exception Throwable" ) );
			}
			return key;
		}
	}
}
