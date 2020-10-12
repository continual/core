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

public class ExpressionEvaluator
{
	public ExpressionEvaluator ( ExprDataSource... srcs )
	{
		fSources = srcs;
	}

	public Object evaluateSymbol ( String expr )
	{
		return evaluateSymbol ( expr, fSources );
	}

	public String evaluateText ( String expr )
	{
		return evaluateText ( expr, fSources );
	}

	/**
	 * Evaluate the given expression against the given data sources and return 
	 * an object. If no source can resolve the symbol, null is returned.
	 * @param symbol
	 * @param srcs a set of data sources, evaluated in order
	 * @return an object if found
	 */
	public static Object evaluateSymbol ( String symbol, ExprDataSource... srcs )
	{
		for ( ExprDataSource src : srcs )
		{
			final Object result = src.eval ( symbol );
			if ( result != null ) return result;
		}
		return null;
	}

	/**
	 * substitute any occurrence of ${&lt;expr&gt;} with the evaluation of that expression 
	 * @param sourceString
	 * @param root
	 * @return a string
	 */
	public static String evaluateText ( String sourceString, ExprDataSource... srcs )
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
					final Object symval = evaluateSymbol ( key, srcs );
					sb.append ( symval == null ? "" : symval.toString () );
					sourceString = sourceString.substring ( closer + 1 );
				}
			}
		}
		while ( sourceString.length () > 0 );
		
		return sb.toString ();
	}

	private final ExprDataSource[] fSources;
}
