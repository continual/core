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

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import junit.framework.TestCase;

public class JsonKeyOrderingTest extends TestCase
{
	@Test
	public void testOrdering ()
	{
		final JSONObject obj = new JSONObject ()
			.put ( JsonUtil.skKeyOrderArray, new JSONArray ().put ( "baz" ).put ( "foot"  ) )
			.put ( "foo", 123 )
			.put ( "foot", 123 )
			.put ( "fool", 123 )
			.put ( "bar", false )
			.put ( "baz", new JSONObject ()
				.put ( "1", "one" )
				.put ( "2", new JSONArray ()
					.put ( new JSONObject ()
						.put ( JsonUtil.skKeyOrderArray, new JSONArray ().put ( "baz" ).put ( "foot"  ) )
						.put ( "inside", true )
					)
					.put ( new JSONObject ()
						.put ( "inside", true )
					)
					.put ( 5.432 )
				)
				.put ( "3", "three" )
			)
		;

		final String jsonText = JsonUtil.writeWithKeyOrder ( obj );
		assertNotNull ( jsonText );
		System.out.println ( jsonText );
	}
}
