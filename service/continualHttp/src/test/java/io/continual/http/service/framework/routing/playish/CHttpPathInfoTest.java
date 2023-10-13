/*
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

package io.continual.http.service.framework.routing.playish;

import org.junit.Test;

import io.continual.http.service.framework.CHttpSession;
import junit.framework.TestCase;

public class CHttpPathInfoTest<C extends CHttpSession> extends TestCase
{
	@Test
	public void testSimplePathParse ()
	{
		final CHttpPathInfo dpi = CHttpPathInfo.processPath ( "GET", "/foo/bar" );
		assertTrue ( null != dpi.matches ( "GET", "/foo/bar" ) );
		assertTrue ( null == dpi.matches ( "GET", "/food/bar" ) );
	}

	@Test
	public void testVariablePathParse ()
	{
		final CHttpPathInfo dpi = CHttpPathInfo.processPath ( "GET", "/foo/{bar}" );
		assertTrue ( null != dpi.matches ( "GET", "/foo/other" ) );

		final CHttpPathInfo dpi2 = CHttpPathInfo.processPath ( "GET", "/foo/{bar}/{baz}/test" );
		assertTrue ( null != dpi2.matches ( "GET", "/foo/v1/v2/test" ) );
		assertTrue ( null == dpi2.matches ( "GET", "/foo/v1/v2/test/more" ) );
	}

	@Test
	public void testVarWithRegexPathParse ()
	{
		final CHttpPathInfo dpi = CHttpPathInfo.processPath ( "GET", "/foo/{<[0-9]>bar}" );
		assertTrue ( null != dpi.matches ( "GET", "/foo/2" ) );
		assertTrue ( null == dpi.matches ( "GET", "/foo/22" ) );
	}

	@Test
	public void testVarWithRegexPathParseAndTrailingPart ()
	{
		final CHttpPathInfo dpi = CHttpPathInfo.processPath ( "GET", "/objects/{<.*>objectId}/types/{typeId}" );
		assertTrue ( null != dpi.matches ( "GET", "/objects/foo/bar/bee/types/t" ) );
		assertFalse ( null != dpi.matches ( "GET", "/objects/foo/bar/bee/typddes/t" ) );
	}

	@Test
	public void testEscapedPath ()
	{
		final CHttpPathInfo dpi = CHttpPathInfo.processPath ( "GET", "/foo/{bar}/bee" );
		assertTrue ( null != dpi.matches ( "GET", "/foo/bar/bee" ) );
		assertFalse ( null != dpi.matches ( "GET", "/foo/ba/r/bee" ) );
		assertTrue ( null != dpi.matches ( "GET", "/foo/ba%2Fr/bee" ) );
	}
}
