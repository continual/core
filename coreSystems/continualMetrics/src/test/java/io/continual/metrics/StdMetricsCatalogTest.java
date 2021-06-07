package io.continual.metrics;

import org.json.JSONObject;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

import io.continual.metrics.impl.StdMetricsCatalog;
import io.continual.metrics.metricTypes.Counter;
import junit.framework.TestCase;

public class StdMetricsCatalogTest extends TestCase
{
	@Test
	public void testContainment ()
	{
		final MetricRegistry reg = new MetricRegistry ();
		final StdMetricsCatalog cat = new StdMetricsCatalog ( reg );
		cat.counter ( "foo" );

		final MetricsCatalog subcat = cat.getSubCatalog ( "sub" );
		subcat.counter ( "bar" );

		final MetricsCatalog subcat2 = subcat.getSubCatalog ( "sub" );
		subcat2.counter ( "baz" );

		final JSONObject top = cat.toJson ();
		final JSONObject bottom = subcat2.toJson ();

		assertEquals ( 2, top.keySet ().size () );
		assertEquals ( 1, bottom.keySet ().size () );
		assertTrue ( bottom.keySet().contains ( "baz" ) );
	}

	@Test
	public void testNameFixes ()
	{
		final MetricRegistry reg = new MetricRegistry ();
		final StdMetricsCatalog cat = new StdMetricsCatalog ( reg );

		final StdMetricsCatalog foo = cat.getSubCatalog ( "foo" );
		assertEquals ( "/foo", foo.getBasePath().toString () );

		final StdMetricsCatalog bar = foo.getSubCatalog ( "bar.baz" );
		assertEquals ( "/foo/bar_baz", bar.getBasePath().toString () );

		final Counter c = bar.counter ( "c" );
		c.increment ();

		final JSONObject o = cat.toJson ();

		assertNotNull ( o.getJSONObject ( "foo" ).getJSONObject ( "bar_baz" ).getJSONObject ( "c" ) );
	}

	@Test
	public void testSubCatalogRemoval ()
	{
		final MetricRegistry reg = new MetricRegistry ();
		final StdMetricsCatalog cat = new StdMetricsCatalog ( reg );
		cat.counter ( "foo" );
		cat.counter ( "s13" );

		final MetricsCatalog subcat = cat.getSubCatalog ( "s1" );
		subcat.counter ( "bar" );
		subcat.counter ( "baz" );

		final MetricsCatalog subcat2 = cat.getSubCatalog ( "s2" );
		subcat2.counter ( "bar" );
		subcat2.counter ( "baz" );

		JSONObject top = cat.toJson ();
		assertEquals ( 4, top.keySet ().size () );

		cat.removeSubCatalog ( "s1" );
		top = cat.toJson ();
		assertEquals ( 3, top.keySet ().size () );
	}
}
