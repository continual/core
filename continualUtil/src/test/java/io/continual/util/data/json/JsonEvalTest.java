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

package io.continual.util.data.json;

import io.continual.util.data.json.JsonEval;
import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class JsonEvalTest extends TestCase
{
	@Test
	public void testSimpleEval ()
	{
		assertEquals ( "foo", JsonEval.eval (
			new JSONObject()
				.put ( "bar","foo" )
			,
			"bar" ) );
		assertNull ( JsonEval.eval (
			new JSONObject()
				.put ( "bar","foo" )
			,
			"ch" ) );
	}

	@Test
	public void testSubobjectEval ()
	{
		assertEquals ( "foo", JsonEval.eval (
			new JSONObject()
				.put ( "bar",
					new JSONObject ()
						.put ( "bee", "foo" )
				)
			,
			"bar.bee" ) );

		assertNull ( JsonEval.eval (
			new JSONObject()
				.put ( "bar",
					new JSONObject ()
						.put ( "bee", "foo" )
				)
			,
			"bar.beet" ) );
	}

	@Test
	public void testArrayEval ()
	{
		assertEquals ( "zugga", JsonEval.eval (
			new JSONObject()
				.put ( "bar",
					new JSONArray ()
						.put (
							new JSONObject ()
								.put ( "ugga", "mugga" )
						)
						.put (
							new JSONObject ()
								.put ( "sugga", "zugga" )
						)
				)
			,
			"bar[1].sugga" ) );
	}

	@Test
	public void testArrayResult ()
	{
		final Object o = JsonEval.eval (
			new JSONObject()
				.put ( "bar",
					new JSONArray ()
						.put (
							new JSONObject ()
								.put ( "ugga", "mugga" )
						)
						.put (
							new JSONObject ()
								.put ( "sugga", "zugga" )
						)
				)
			,
			"bar" );
		assertTrue ( o instanceof JSONArray );
		final JSONArray a = (JSONArray) o;
		assertEquals ( 2, a.length() );
	}

	@Test
	public void testSet ()
	{
		final JSONObject start = new JSONObject ();
		JsonEval.setValue ( start, "foo.bar.bee", 3 );
		assertEquals ( 3, JsonEval.eval ( start, "foo.bar.bee" ) );

		JsonEval.setValue ( start, "foo.bar.baz", new JSONObject ().put ( "carrot", "orange" ) );
		final JSONObject baz = start.getJSONObject ( "foo" ).getJSONObject ( "bar" ).getJSONObject ( "baz" );
		assertEquals ( "orange", baz.get ( "carrot" ));

		assertTrue ( JsonEval.hasKey ( start, "foo.bar.bee" ) );
		assertTrue ( JsonEval.hasKey ( start, "foo.bar.baz" ) );
		assertTrue ( JsonEval.hasKey ( start, "foo.bar.baz.carrot" ) );
		assertFalse ( JsonEval.hasKey ( start, "foo.bar.baz.beet" ) );
		assertFalse ( JsonEval.hasKey ( start, "foo.beast.baz.beet" ) );
	}
}
