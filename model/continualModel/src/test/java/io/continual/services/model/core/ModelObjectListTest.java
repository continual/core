package io.continual.services.model.core;

import java.util.Iterator;

import org.json.JSONObject;
import org.junit.Test;

import io.continual.services.model.core.data.BasicModelObject;
import io.continual.services.model.core.data.JsonObjectAccess;
import io.continual.services.model.impl.json.CommonModelObjectMetadata;
import io.continual.util.naming.Path;
import junit.framework.TestCase;

public class ModelObjectListTest extends TestCase
{
	@Test
	public void testEmptyList ()
	{
		final ModelObjectList<?> mol = ModelObjectList.emptyList ();
		assertNotNull ( mol );
		assertNotNull ( mol.iterator () );
		assertFalse ( mol.iterator ().hasNext () );
	}

	@Test
	public void testEmptyListAsSimpleList ()
	{
		final ModelObjectList<?> mol = ModelObjectList.simpleList ();
		assertNotNull ( mol );
		assertNotNull ( mol.iterator () );
		assertFalse ( mol.iterator ().hasNext () );
	}

	@Test
	public void testOneItemList ()
	{
		final BasicModelObject m1 = makeObj ();
		final ModelObjectList<BasicModelObject> mol = ModelObjectList.simpleList ( mp ( "/p1", m1 ) );
		assertNotNull ( mol );
		
		final Iterator<ModelObjectAndPath<BasicModelObject>> iter = mol.iterator ();
		assertNotNull ( iter );
		assertTrue ( iter.hasNext () );
		assertEquals ( m1, iter.next ().getObject () );
		assertFalse ( iter.hasNext () );
	}

	@Test
	public void testMultiItemList ()
	{
		final BasicModelObject m1 = makeObj ();
		final BasicModelObject m2 = makeObj ();
		final BasicModelObject m3 = makeObj ();
		final ModelObjectList<BasicModelObject> mol = ModelObjectList.simpleList ( mp ( "/p1", m1 ), mp ( "/p2", m2 ), mp ( "/p3", m3 ) );
		assertNotNull ( mol );
		
		final Iterator<ModelObjectAndPath<BasicModelObject>> iter = mol.iterator ();
		assertNotNull ( iter );
		assertTrue ( iter.hasNext () );
		assertEquals ( m1, iter.next ().getObject () );
		assertTrue ( iter.hasNext () );
		assertEquals ( m2, iter.next ().getObject () );
		assertTrue ( iter.hasNext () );
		assertEquals ( m3, iter.next ().getObject () );
		assertFalse ( iter.hasNext () );
	}

	@Test
	public void testSeparateIteration ()
	{
		final BasicModelObject m1 = makeObj ();
		final BasicModelObject m2 = makeObj ();
		final BasicModelObject m3 = makeObj ();
		final ModelObjectList<BasicModelObject> mol = ModelObjectList.simpleList ( mp ( "/p1", m1 ), mp ( "/p2", m2 ), mp ( "/p3", m3 ) );
		assertNotNull ( mol );

		final Iterator<ModelObjectAndPath<BasicModelObject>> iter1 = mol.iterator ();
		final Iterator<ModelObjectAndPath<BasicModelObject>> iter2 = mol.iterator ();

		iter1.next ();
		iter1.next ();
		iter1.next ();

		assertEquals ( m1, iter2.next ().getObject () );
	}

	private static ModelObjectAndPath<BasicModelObject> mp ( String p, BasicModelObject mo )
	{
		return ModelObjectAndPath.from ( Path.fromString ( p ), mo );
	}

	private static BasicModelObject makeObj ()
	{
		return new BasicModelObject ( Path.getRootPath (), new CommonModelObjectMetadata(), new JsonObjectAccess ( new JSONObject () ) );
	}
}
