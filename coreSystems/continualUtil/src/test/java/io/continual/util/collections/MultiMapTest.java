package io.continual.util.collections;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Collection;

public class MultiMapTest extends TestCase
{
	private final Map<String, List<String>> data = new HashMap<String, List<String>> ();
	private final int maxSize = 5;

	@Override
	protected void setUp () throws Exception
	{
		final LinkedList<String> ll = new LinkedList<String> ();
		for( int index=0; index<maxSize; index++ )
		{
			ll.add( "value"+index );
		}
		for( int index=0; index<maxSize-1; index++ )
		{
			data.put( "key"+index , ll );
		}
		data.put( "key"+(maxSize-1) , new LinkedList<String> () );
	}

	@Test
	public void testMultiMapArgsConstructor ()
	{
		final Map<String, String> mapData = new HashMap<String, String> ();
		for( int index=0; index<maxSize; index++ )
		{
			mapData.put( "key"+index , "value"+index );
		}
		final MultiMap<String, String> mm = new MultiMap<String, String> ( mapData );

		assertEquals( maxSize, mm.size () );
		assertTrue( mm.containsKey( "key0" ) );
	}

	@Test
	public void testPut ()
	{
		final MultiMap<String, String> mm = new MultiMap<String, String> ();
		mm.put( "key0" );

		assertEquals( 0 , mm.size( "key0" ) );
	}

	@Test
	public void testPutAll ()
	{
		final MultiMap<String, String> mm = new MultiMap<String, String> ();
		mm.putAll( data );

		assertEquals( maxSize , mm.get( "key0" ).size() );
		assertEquals( maxSize , mm.getKeys().size() );
		assertEquals( maxSize , mm.getValues().size() );
		assertEquals( maxSize , mm.size( "key0" ) );
		assertTrue( mm.get( "key"+maxSize ).isEmpty() ); // Test get not contains key
	}

	@Test
	public void testGetFirst ()
	{
		final MultiMap<String, String> mm = new MultiMap<String, String> ();
		mm.putAll( data );

		assertEquals( "value0" , mm.getFirst( "key0" ) );
		assertNull( mm.getFirst( "key"+(maxSize-1) ) );		
	}

	@Test
	public void testRemoveClear ()
	{
		final MultiMap<String, String> mm = new MultiMap<String, String> ();
		mm.putAll( data );
		mm.toString();

		mm.remove( "key0" );
		assertFalse( mm.containsKey( "key0" ) );
		mm.remove( "key1" , "value0" );
		assertEquals( "value1" , mm.getFirst( "key1" ) );
		mm.clear();
		assertEquals( 0 ,  mm.size() );
	}

	@Test
	public void testClone ()
	{
		final MultiMap<String, String> mm = new MultiMap<String, String> ();
		mm.putAll( data );

		final MultiMap<String, String> mmClone = mm.clone();
		assertEquals( maxSize , mmClone.size() );
	}

	@Test
	public void testGetCopyAsSimpleMap ()
	{
		final MultiMap<String, String> mm = new MultiMap<String, String> ();
		mm.putAll( data );

		final Map<String, Collection<String>> dataCopy = mm.getCopyAsSimpleMap();
		assertEquals( maxSize , dataCopy.size() );
	}

	@Override
	protected void tearDown () throws Exception
	{
		data.clear();
	}
	
}
