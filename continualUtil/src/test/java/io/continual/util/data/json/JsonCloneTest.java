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

import io.continual.util.data.json.JsonUtil;
import org.json.JSONObject;
import org.junit.Test;

import junit.framework.TestCase;

public class JsonCloneTest extends TestCase
{
	@Test
	public void testClone ()
	{
		final JSONObject o = new JSONObject ()
			.put ( "foo", "bar" )
			.put ( "one", 1 )
			.put ( "sub", new JSONObject ().put ( "blat", "flat" ) )
		;
		final JSONObject that = JsonUtil.clone ( o );
		assertFalse ( that == o );
		assertEquals ( "flat", that.getJSONObject ( "sub" ).getString ( "blat" ) );
		o.getJSONObject ( "sub" ).put ( "blat", "rat" );
		assertEquals ( "flat", that.getJSONObject ( "sub" ).getString ( "blat" ) );
	}
}
