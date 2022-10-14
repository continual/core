package io.continual.services.model.core;

import java.util.Iterator;

import org.junit.Test;

import io.continual.services.model.impl.json.CommonJsonDbObject;
import junit.framework.TestCase;

public class ModelObjectListTest extends TestCase
{
	@Test
	public void testEmptyList ()
	{
		final ModelObjectList mol = ModelObjectList.emptyList ();
		assertNotNull ( mol );
		assertNotNull ( mol.iterator () );
		assertFalse ( mol.iterator ().hasNext () );
	}

	@Test
	public void testEmptyListAsSimpleList ()
	{
		final ModelObjectList mol = ModelObjectList.simpleList ();
		assertNotNull ( mol );
		assertNotNull ( mol.iterator () );
		assertFalse ( mol.iterator ().hasNext () );
	}

	@Test
	public void testOneItemList ()
	{
		final ModelObject m1 = new CommonJsonDbObject ();
		final ModelObjectList mol = ModelObjectList.simpleList ( m1 );
		assertNotNull ( mol );
		
		final Iterator<ModelObject> iter = mol.iterator ();
		assertNotNull ( iter );
		assertTrue ( iter.hasNext () );
		assertEquals ( m1, iter.next () );
		assertFalse ( iter.hasNext () );
	}

	@Test
	public void testMultiItemList ()
	{
		final ModelObject m1 = new CommonJsonDbObject ();
		final ModelObject m2 = new CommonJsonDbObject ();
		final ModelObject m3 = new CommonJsonDbObject ();
		final ModelObjectList mol = ModelObjectList.simpleList ( m1, m2, m3 );
		assertNotNull ( mol );
		
		final Iterator<ModelObject> iter = mol.iterator ();
		assertNotNull ( iter );
		assertTrue ( iter.hasNext () );
		assertEquals ( m1, iter.next () );
		assertTrue ( iter.hasNext () );
		assertEquals ( m2, iter.next () );
		assertTrue ( iter.hasNext () );
		assertEquals ( m3, iter.next () );
		assertFalse ( iter.hasNext () );
	}

	@Test
	public void testSeparateIteration ()
	{
		final ModelObject m1 = new CommonJsonDbObject ();
		final ModelObject m2 = new CommonJsonDbObject ();
		final ModelObject m3 = new CommonJsonDbObject ();
		final ModelObjectList mol = ModelObjectList.simpleList ( m1, m2, m3 );
		assertNotNull ( mol );
		
		final Iterator<ModelObject> iter1 = mol.iterator ();
		final Iterator<ModelObject> iter2 = mol.iterator ();

		iter1.next ();
		iter1.next ();
		iter1.next ();

		assertEquals ( m1, iter2.next () );
	}
}
