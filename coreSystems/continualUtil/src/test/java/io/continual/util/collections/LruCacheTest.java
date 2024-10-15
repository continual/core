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
package io.continual.util.collections;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.util.collections.LruCache.ExpulsionListener;
import junit.framework.TestCase;

public class LruCacheTest extends TestCase
{
	@Test
	public void testCache ()
	{
		final long maxSize = 10;
		final LruCache<String,String> c = new LruCache<String,String> ( maxSize );

		assertEquals ( maxSize, c.maxSize () );

		assertNull ( c.lookup ( "k1" ) );

		c.store ( "k1", "1" );
		assertEquals ( "1", c.lookup ( "k1" ) );

		c.drop ( "k1" );
		assertNull ( c.lookup ( "k1" ) );

		for ( int i=0; i<20; i++ )
		{
			c.store ( "k"+i, ""+i );
		}
		assertNull ( c.lookup ( "k1" ) );
		assertEquals ( "19", c.lookup ( "k19" ) );

		c.clear ();
		assertNull ( c.lookup ( "k19" ) );
	}

	@Test
	public void testCacheSizing ()
	{
		final long maxSize = 1;
		final LruCache<String,String> c = new LruCache<String,String> ( maxSize );
		assertEquals ( maxSize, c.maxSize () );

		c.setMaxSize ( 0 );
		assertEquals ( 0, c.maxSize () );

		c.setMaxSize ( -10 );
		assertEquals ( 0, c.maxSize () );

		c.store ( "k1", "1" );
		assertNull ( c.lookup ( "k1" ) );
	}

	@Test
	public void testClearExpulsions()
	{
		final int maxSize = 10;
		final LruCache<String,String> cache = new LruCache<String,String> ( maxSize );
		for( int index = 0; index < maxSize; index++) {
			cache.store ( "k" + index, "" + index, new TestExpulsionListener () );
		}

		assertFalse( cache.isEmpty() );
		assertTrue( cache.containsKey( "k0") );
		assertEquals( "0", cache.get( "k0" ) );
		assertNotNull( cache.keys() );

		assertNull( cache.remove( "k"+(maxSize+1) ) );
		cache.clear(true);

		assertTrue( cache.isEmpty() );
		assertFalse( cache.containsKey( "k0") );
	}

	@Test
	public void testEnsureCapacity()
	{
		final LruCache<String,String> cache = new LruCache<String,String> ( 2 );
		cache.store ( "k0", "0", new TestExpulsionListener () );
		cache.put ( "k1", "1" );
		cache.store ( "k2", "2", new TestExpulsionListener () );

		assertEquals( "2", cache.get( "k2" ) );
	}

	@Test
	public void testLookup()
	{
		final int maxSize = 10;
		final LruCache<String,String> cache = new LruCache<String,String> ( maxSize );
		for( int index=0; index<maxSize; index++ )
		{
			cache.store ( "k"+index, ""+index, new TestExpulsionListener () );
		}

		assertNotNull( cache.get( "k"+(maxSize-1) , 10000 ) );		// age < maxAgeMs

		try {
			Thread.sleep(100);
		} catch( InterruptedException ie) {}
		assertNull( cache.get( "k0" , 0 ) );		// age > maxAgeMs
	}

	private class TestExpulsionListener implements ExpulsionListener<String, String>
	{
		public void onExpelled( String key , String value )
		{
			// Empty Implementation
		}
	}

	//@Test
	public void _testLongRunPerf ()
	{
		final LruCache<String,String> cache = new LruCache<String,String> ( 4096 );
		final int useCount = 1000*1000*10;

		final Runtime rt = Runtime.getRuntime ();

		final int reportInterval = 10000;
		long internalStartMs = System.currentTimeMillis ();

		for ( int i=0; i<useCount; i++ )
		{
			cache.put ( "key_" + i, "data" );
			if ( i % reportInterval == 0 )
			{
				final long intervalEndMs = System.currentTimeMillis ();
				final long durationMs = intervalEndMs - internalStartMs;
				final long totalMem = rt.totalMemory () - rt.freeMemory ();
				log.info ( "index {}, last interval {} ms, cache size {}, memory {} MB", i, durationMs, cache.size (), ( totalMem / (1024*1024) ) );

				internalStartMs = System.currentTimeMillis ();
			}
		}
	}

	private static final Logger log = LoggerFactory.getLogger ( LruCacheTest.class );
}
