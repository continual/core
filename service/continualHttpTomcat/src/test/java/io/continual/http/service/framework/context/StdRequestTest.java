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

package io.continual.http.service.framework.context;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import junit.framework.TestCase;

public class StdRequestTest extends TestCase
{
	@Test
	public void testPath ()
	{
		final HttpServletRequest base = new MockServletRequest ()
		{
			@Override
			public String getPathInfo () { return "foo/bar/bee"; }

			@Override
			public String getContextPath () { return "/context"; }

			@Override
			public String getRequestURI () { return "/context/foo/bar%2Fbee"; }
			
			@Override
			public StringBuffer getRequestURL () { return new StringBuffer().append( "https://foo.bar" ); }
		};

		final StdRequest req = new StdRequest ( base );
		final String pic = req.getPathInContext ();
		assertEquals ( "/foo/bar%2Fbee", pic );
	}
}
