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

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import junit.framework.TestCase;

public class JsonPathEvalTest extends TestCase
{
	@Test
	public void testJsonPath ()
	{
		final JSONObject root = new JSONObject ()
			.put ( "store", new JSONObject ()
				.put ( "book", new JSONArray ()
					.put ( new JSONObject ()
						.put ( "isbn", "0-000-00000-0" )
						.put ( "category", "reference" )
					)
					.put ( new JSONObject ()
						.put ( "isbn", "0-000-00000-1" )
						.put ( "category", "fiction" )
					)
					.put ( new JSONObject ()
						.put ( "isbn", "0-000-00000-2" )
						.put ( "category", "fiction" )
					)
					.put ( new JSONObject ()
						.put ( "isbn", "0-000-00000-3" )
						.put ( "category", "fiction" )
					)
				)
			)
		;

		final String jp = "$.store.book[?(@.category=='fiction')]";

		final List<String> result = JsonPathEval.evaluateJsonPath ( root, jp );
		assertEquals ( 3, result.size () );
	}
}
