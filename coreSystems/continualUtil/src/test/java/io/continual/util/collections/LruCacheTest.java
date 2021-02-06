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

import junit.framework.TestCase;

import org.junit.Test;

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
}
