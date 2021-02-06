package io.continual.metrics;

import org.json.JSONObject;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

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
}
