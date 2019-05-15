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

import org.json.JSONObject;
import org.junit.Test;

import io.continual.util.data.json.CommentedJsonTokener;

import junit.framework.TestCase;

public class CommentedJsonTokenerTest extends TestCase
{
	@Test
	public void testStripper ()
	{
		final JSONObject o = new JSONObject ( new CommentedJsonTokener ( "{'foo':'bar' /* bee */ }" ) );
		assertNotNull ( o );
		assertTrue ( o.has ( "foo" ) );
		assertEquals ( "bar", o.getString ( "foo" ) );
	}

	@Test
	public void testCommentInStringBody ()
	{
		final JSONObject o = new JSONObject ( new CommentedJsonTokener ( "{'foo':'bar /* bee */' }" ) );
		assertNotNull ( o );
		assertTrue ( o.has ( "foo" ) );
		assertEquals ( "bar /* bee */", o.getString ( "foo" ) );
	}

	@Test
	public void testMultilineBlockComment ()
	{
		final JSONObject o = new JSONObject ( new CommentedJsonTokener ( "{'foo':'bar', /* bee\nbar\n// blue */ 'bee':'baz' }" ) );
		assertNotNull ( o );
		assertEquals ( "bar", o.getString ( "foo" ) );
		assertEquals ( "baz", o.getString ( "bee" ) );
	}
}
