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

package io.continual.http.app.htmlForms;

import java.util.HashMap;

import org.junit.Test;

import io.continual.http.app.htmlForms.CHttpFormPostWrapper.ParseException;
import io.continual.http.service.framework.CHttpConnection;
import junit.framework.TestCase;

public class FormValidatorTest<C extends CHttpConnection> extends TestCase
{
	@Test
	public void testRequiredField () throws CHttpInvalidFormException, ParseException
	{
		final CHttpFormValidator v = new CHttpFormValidator ();
		v.field ( "test" ).matches ( ".*", "anything" ).defaultValue ( "hiya" );

		final HashMap<String,String[]> map = new HashMap<>();
		final CHttpFormPostWrapper w = new CHttpFormPostWrapper ( new MockRequest ( map ) );
		final MockHandlingContext<C> ctx = new MockHandlingContext<>();
		v.validate ( ctx, w );

		assertEquals ( "hiya", w.getValue ( "test" ) );
	}

	@Test
	public void testValidation () throws CHttpInvalidFormException
	{
		final CHttpFormValidator v = new CHttpFormValidator ();
		v.field ( "test" ).required ("").matches ( ".*", "anything" ).defaultValue ( "hiya" );

		final HashMap<String,String[]> map = new HashMap<>();
		map.put ( "test", new String[] { "fubar" } );
		final CHttpFormPostWrapper w = new CHttpFormPostWrapper ( new MockRequest ( map ) );

		final MockHandlingContext<C> ctx = new MockHandlingContext<>();
		v.validate ( ctx, w );
	}

	@Test
	public void testMissingField ()
	{
		final CHttpFormValidator v = new CHttpFormValidator ();
		v.field ( "test" ).required ("");

		final HashMap<String,String[]> map = new HashMap<>();
		map.put ( "test", new String[] { "" } );
		final CHttpFormPostWrapper w = new CHttpFormPostWrapper ( new MockRequest ( map ) );

		try
		{
			final MockHandlingContext<C> ctx = new MockHandlingContext<>();
			v.validate ( ctx, w );
			fail ( "Reqd field is missing" );
		}
		catch ( CHttpInvalidFormException e )
		{
			/* expected exception */
		}
	}

	@Test
	public void testMissingFieldWithDefault ()
	{
		final CHttpFormValidator v = new CHttpFormValidator ();
		v.field ( "test" ).defaultValue ( "hiya" );

		final HashMap<String,String[]> map = new HashMap<>();
		final CHttpFormPostWrapper w = new CHttpFormPostWrapper ( new MockRequest ( map ) );

		try
		{
			final MockHandlingContext<C> ctx = new MockHandlingContext<>();
			v.validate ( ctx, w );
		}
		catch ( CHttpInvalidFormException e )
		{
			fail ( "Reqd field is missing but has default" );
		}
	}

	@Test
	public void testRegexMatches ()
	{
		final CHttpFormValidator v = new CHttpFormValidator ();
		v.field ( "test" ).matches ( "bar|bee", "Test must be bar or bee." );

		final HashMap<String,String[]> map = new HashMap<>();
		map.put ( "test", new String[] { "bar" } );
		final CHttpFormPostWrapper w = new CHttpFormPostWrapper ( new MockRequest ( map ) );

		try
		{
			final MockHandlingContext<C> ctx = new MockHandlingContext<>();
			v.validate ( ctx, w );
		}
		catch ( CHttpInvalidFormException e )
		{
			fail ( "regex matches" );
		}
	}

	@Test
	public void testRegexDoesntMatch ()
	{
		final CHttpFormValidator v = new CHttpFormValidator ();
		v.field ( "test" ).matches ( "bar|bee", "Test must be bar or bee." );

		final HashMap<String,String[]> map = new HashMap<>();
		map.put ( "test", new String[] { "" } );
		final CHttpFormPostWrapper w = new CHttpFormPostWrapper ( new MockRequest ( map ) );

		try
		{
			final MockHandlingContext<C> ctx = new MockHandlingContext<>();
			v.validate ( ctx, w );
			fail ( "field doesn't match" );
		}
		catch ( CHttpInvalidFormException e )
		{
			assertEquals ( "Test must be bar or bee.", e.getFieldProblems ().get ( "test" ).iterator ().next () );
		}
	}
}
