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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.json.JSONObject;
import org.junit.Test;

public class CommentedJsonTokenerTest
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
	
	@Test
	public void createWithInputStream (){
		InputStream input = new ByteArrayInputStream("{'id':12, 'name':'arzu' }".getBytes());
		final JSONObject o = new JSONObject ( new CommentedJsonTokener ( input ) );
		assertNotNull ( o );
		assertEquals ( 12, o.getInt("id") );
		assertEquals ( "arzu", o.getString ( "name" ) );
	}
	
	@Test
	public void createWithReader (){
		Reader input = new StringReader("{'id':12, 'name':'arzu' }");
		final JSONObject o = new JSONObject ( new CommentedJsonTokener ( input ) );
		assertNotNull ( o );
		assertEquals ( 12, o.getInt("id") );
		assertEquals ( "arzu", o.getString ( "name" ) );
	}
	
	@Test
	public void complexExample (){
		final JSONObject o = new JSONObject ( new CommentedJsonTokener ( "{\"id\":'er\\'d', \'name':\"ar\\\"zu\" }" ) );
		assertNotNull ( o );
		assertTrue ( o.has ( "id" ) );
		assertEquals ( "er'd", o.getString ( "id" ) );
		assertTrue ( o.has ( "name" ) );
		assertEquals ( "ar\"zu", o.getString ( "name" ) );
	}
	
	@Test
	public void complexExample2 (){
		final JSONObject o = new JSONObject ( new CommentedJsonTokener ( "{\"id\":'test' //comment\n  }" ) );
		assertNotNull ( o );
		assertTrue ( o.has ( "id" ) );
		assertEquals ( "test", o.getString ( "id" ) );
	}
	
//	@Test
	public void complexExample3 (){
		final JSONObject o = new JSONObject ( new CommentedJsonTokener ( "{\"id\":'test' /comment\n  }" ) );
		assertNotNull ( o );
		assertTrue ( o.has ( "id" ) );
		assertEquals ( "test", o.getString ( "id" ) );
	}
	
//	@Test
	public void complexExample4 (){
		final JSONObject o = new JSONObject ( new CommentedJsonTokener ( "{\"id\":'test'  /*comment*\n  }" ) );
		assertNotNull ( o );
		assertTrue ( o.has ( "id" ) );
		assertEquals ( "test", o.getString ( "id" ) );
	}
	
//	@Test
	public void complexExample5 (){
		final JSONObject o = new JSONObject ( new CommentedJsonTokener ( "{\"id\":'test'  /*comment*p\n  }" ) );
		assertNotNull ( o );
		assertTrue ( o.has ( "id" ) );
		assertEquals ( "test", o.getString ( "id" ) );
	}
	
	@Test
	public void complexExample_EOF (){
		final JSONObject o = new JSONObject ( new CommentedJsonTokener ( "{\"id\":\"test\" \n  }" ) );
		assertNotNull ( o );
		assertTrue ( o.has ( "id" ) );
		assertEquals ( "test", o.getString ( "id" ) );
	}
	
}
