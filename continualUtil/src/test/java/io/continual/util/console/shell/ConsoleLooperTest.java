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

package io.continual.util.console.shell;

import io.continual.util.console.shell.ConsoleLooper;
import junit.framework.TestCase;

import org.junit.Test;

public class ConsoleLooperTest extends TestCase
{
	private static final String[][] kSplitTests =
	{
		new String[] { "foo", "foo" },
		new String[] { "a b", "a", "b" },
		new String[] { "a \"b c\" d", "a", "b c", "d" },
		new String[] { "a\"bc\"d", "a\"bc\"d" },
		new String[] { "{\"foo\":\"bar\"}", "{\"foo\":\"bar\"}" },
		new String[] { "\"{ 'foo': 'bar' }\"", "{ 'foo': 'bar' }" },
	};
	
	@Test
	public void testLineSplits ()
	{
		for ( String[] test : kSplitTests )
		{
			final String[] result = ConsoleLooper.splitLine ( test[0] );
			assertEquals ( test.length-1, result.length );
			for ( int i=0; i<result.length; i++ )
			{
				assertEquals ( test[i+1], result[i] );
			}
		}
	}
}
