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

package io.continual.util.data.exprEval;

import java.util.Map;

public class ExpressionEvaluator
{
	/**
	 * Evaluate the given expression against the given root object and return 
	 * a string representation. If the evaluation is null, an empty string is returned.
	 * @param root
	 * @param expression
	 * @return a string
	 */
	public static String evalToString ( ExprDataSource root, String expression )
	{
		final Object result = root.eval ( expression );
		if ( result == null ) return "";
		return result.toString ();
	}

	/**
	 * eval to string using a string map as a data source
	 * @param map
	 * @param expression
	 * @return a string
	 */
	public static String evalToString ( final Map<String,String> map, String expression )
	{
		return evalToString ( new ExprDataSource(){

			@Override
			public Object eval ( String label )
			{
				return map.get ( label );
			}
			
		}, expression );
	}

	/**
	 * substitute any occurrence of ${&lt;expr&gt;} with the evaluation of that expression 
	 * @param sourceString
	 * @param root
	 * @return a string
	 */
	public static String evaluate ( String sourceString, ExprDataSource root )
	{
		if ( sourceString == null ) return null;

		final StringBuffer sb = new StringBuffer ();
		do
		{
			final int open = sourceString.indexOf ( "${" );
			if ( open < 0 )
			{
				// just straight text left
				sb.append ( sourceString );
				sourceString = "";
			}
			else
			{
				// read to "}", use the content as a key into the json
				final int closer = sourceString.indexOf ( '}' );
				if ( closer < 0 )
				{
					// not found. just treat it like plain text
					sb.append ( sourceString );
					sourceString = "";
				}
				else
				{
					sb.append ( sourceString.substring ( 0, open ) );
					final String key = sourceString.substring ( open+2, closer );
					sb.append ( evalToString ( root, key ) );
					sourceString = sourceString.substring ( closer + 1 );
				}
			}
		}
		while ( sourceString.length () > 0 );
		
		return sb.toString ();
	}
}
