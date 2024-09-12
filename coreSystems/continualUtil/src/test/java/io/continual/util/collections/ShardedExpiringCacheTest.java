/*
 *	Copyright 2021, Continual.io
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
package io.continual.util.collections;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.collections.ShardedExpiringCache.Fetcher;
import io.continual.util.collections.ShardedExpiringCache.Fetcher.FetchException;
import io.continual.util.collections.ShardedExpiringCache.Monitor;
import io.continual.util.collections.ShardedExpiringCache.Validator;
import junit.framework.TestCase;

public class ShardedExpiringCacheTest extends TestCase
{
	@Test
	public void testCache () throws FetchException, InterruptedException
	{
		final ShardedExpiringCache<String,String> c = new ShardedExpiringCache.Builder<String,String> ()
			.withShardCount ( 1024 )
			.build ()
		;

		assertNull ( c.read ( "foo" ) );

		c.write ( "foo", "bar" );
		assertEquals ( "bar", c.read ( "foo" ) );
		assertNull ( c.read ( "baz" ) );

		assertEquals ( "BAZ", c.read ( "baz", null, new Fetcher<String,String> ()
		{
			public String fetch ( String key ) 
			{
				return key.toUpperCase ();
			}
		} ) );
		assertEquals ( "BAZ", c.read ( "baz" ) );
	}

	@Test
	public void testCallFlood () throws FetchException, InterruptedException
	{
		final ShardedExpiringCache<String,String> c = new ShardedExpiringCache.Builder<String,String> ()
			.withShardCount ( 1024 )
			.build ()
		;

		final AtomicBoolean runFlag = new AtomicBoolean ( false );
		final int n = 400;
		final ExecutorService execs = Executors.newFixedThreadPool ( n/2 );

		for ( int i=0; i<n; i++ )
		{
			final int thisThreadId = i;
			execs.submit ( new Runnable ()
			{
				@Override
				public void run ()
				{
					try
					{
						final String val = c.read ( "key", null, new TestFetch ( runFlag ) );
						assertEquals ( kMagicValue, val );
						log.info ( "thread {}: {}", thisThreadId, val );
					}
					catch ( FetchException e )
					{
						fail ( "fetch fail: " + e.getMessage () );
					}
				}
			} );
		}

		execs.shutdown ();
		assertTrue ( execs.awaitTermination ( 30, TimeUnit.SECONDS ) );
	}

	@Test
	public void testBuilder()
	{
		final ShardedExpiringCache<String,String> secCachingFor = new ShardedExpiringCache.Builder<String,String> ()
				.cachingFor ( 12 , TimeUnit.SECONDS )
				.build ()
			;
		final ShardedExpiringCache<String,String> secNamed = new ShardedExpiringCache.Builder<String,String> ()
				.named ( "named" )
				.build ()
			;
		final ShardedExpiringCache<String,String> secNotifyTo = new ShardedExpiringCache.Builder<String,String> ()
				.notificationsTo ( new TestMonitor () )
				.build ()
			;

		secCachingFor.write ( "fooCF" , "barCF" );
		secNamed.write ( "fooN" , "barN" );
		secNotifyTo.write ( "fooNT" , "barNT" );
		secNotifyTo.write ( "fooNT2" , "invalid" );

		assertTrue( secCachingFor.containsKey( "fooCF" ) );
		assertTrue( secNamed.containsKey( "fooN" , null ) );
		assertFalse( secNamed.containsKey( "fooWrong" , null ) );
		assertTrue( secNotifyTo.containsKey( "fooNT" , new TestValidator () ) );
		assertNull( secNotifyTo.read( "fooNT2" , new TestValidator () ) );

		secCachingFor.remove( "fooCF" );
		secNamed.empty();
		assertFalse( secNamed.containsKey( "fooN" ) );
	}

	@Test
	public void testShardCleanUpCache ()
	{
		final ShardedExpiringCache<String,String> secCachingFor = new ShardedExpiringCache.Builder<String,String> ()
				.cachingFor ( 12 , TimeUnit.SECONDS )
				.build ()
			;
		secCachingFor.write ( "fooCFtimer" , "barCFtimer", 1L );	// Timer

		try {
			Thread.sleep(10);
		} catch(InterruptedException ie) {}
		assertFalse( secCachingFor.containsKey( "fooCFtimer" ) );	// Timer Expires
	}

	@Test
	public void testGc () throws FetchException, InterruptedException
	{
		final ShardedExpiringCache<String,String> c = new ShardedExpiringCache.Builder<String,String> ()
			.withShardCount ( 32 )
			.build ()
		;

		assertNull ( c.read ( "foo" ) );

		c.write ( "foo", "bar" );
		assertEquals ( "bar", c.read ( "foo" ) );

		c.$testDropWeakRef ( "foo" );

		final String postGc = c.read ( "foo" );
		assertNull ( postGc );
	}

	//@Test
	public void _testLongRunPerf ()
	{
		final ShardedExpiringCache<String,String> cache = new ShardedExpiringCache.Builder<String,String> ()
			.named ( "test" )
			.cachingFor ( 15, TimeUnit.MINUTES )
			.withShardMaxSize ( 128 )
			.withShardCount ( 32 )
			.build ()
		;

		final int useCount = 1000*1000*10;

		final Runtime rt = Runtime.getRuntime ();

		final int reportInterval = 10000;
		long internalStartMs = System.currentTimeMillis ();

		for ( int i=0; i<useCount; i++ )
		{
			cache.write ( "key_" + i, "data" );
			if ( i % reportInterval == 0 )
			{
				final long intervalEndMs = System.currentTimeMillis ();
				final long durationMs = intervalEndMs - internalStartMs;
				final long totalMem = rt.totalMemory () - rt.freeMemory ();
				log.info ( "index {}, last interval {} ms, cache size {}, memory {} MB", i, durationMs, cache.size (), ( totalMem / (1024*1024) ) );

				internalStartMs = System.currentTimeMillis ();
			}
		}

		final long intervalEndMs = System.currentTimeMillis ();
		final long durationMs = intervalEndMs - internalStartMs;
		final long totalMem = rt.totalMemory () - rt.freeMemory ();
		log.info ( "index {}, last interval {} ms, cache size {}, memory {} MB", useCount, durationMs, cache.size (), ( totalMem / (1024*1024) ) );
	}

	private static final String kMagicValue = "fetched value";
	private static final Logger log = LoggerFactory.getLogger ( ShardedExpiringCacheTest.class );
	
	private static class TestFetch implements Fetcher<String,String>
	{
		public TestFetch ( AtomicBoolean flag )
		{
			fFlag = flag;
		}
	
		@Override
		public String fetch ( String key ) throws FetchException
		{
			try
			{
				Thread.sleep ( 500 );
			}
			catch ( InterruptedException e )
			{
				throw new FetchException ( e );
			}

			if ( !fFlag.compareAndSet ( false, true ) )
			{
				// double fetch call
				fail ( "double fetch" );
			}
			return kMagicValue;
		}

		private final AtomicBoolean fFlag;
	}

	private static class TestMonitor implements Monitor
	{
		// Empty Class Implementation
	}

	private static class TestValidator implements Validator<String>
	{
		public boolean isValid( String value )
		{
			return "invalid".equals(value) ? false : true;	// Dummy Method Implementation
		}
	}
}
