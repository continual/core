package io.continual.services.model.core;

import java.util.LinkedList;

import org.junit.Test;

import junit.framework.TestCase;

public class PagingIterWrapperTest extends TestCase
{
	@Test
	public void testNoRestriction ()
	{
		final LinkedList<String> list = new LinkedList<> ();
		list.add ( "a" );
		list.add ( "b" );
		list.add ( "c" );

		final PagingIterWrapper<String> wrap = new PagingIterWrapper<> ( list.iterator (), new PageRequest () );
		assertTrue ( wrap.hasNext () );
		assertEquals ( "a", wrap.next () );
		assertTrue ( wrap.hasNext () );
		assertEquals ( "b", wrap.next () );
		assertTrue ( wrap.hasNext () );
		assertEquals ( "c", wrap.next () );
		assertFalse ( wrap.hasNext () );
	}

	@Test
	public void testPageTwo ()
	{
		final LinkedList<String> list = new LinkedList<> ();
		list.add ( "a" );
		list.add ( "b" );
		list.add ( "c" );

		final PagingIterWrapper<String> wrap = new PagingIterWrapper<> ( list.iterator (), new PageRequest ().startingAtPage ( 1 ).withPageSize ( 1 ) );
		assertTrue ( wrap.hasNext () );
		assertEquals ( "b", wrap.next () );
		assertFalse ( wrap.hasNext () );
	}
}
