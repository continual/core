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

import io.continual.util.data.TypeConvertor;

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

	public int evaluateTextToInt ( Object value, int defaultValue )
	{
		return evaluateTextToInt ( value, defaultValue, fSources );
	}
	
	public boolean evaluateTextToBoolean ( Object value, boolean defaultValue )
	{
		return evaluateTextToBoolean ( value, defaultValue, fSources );
	}

	/**
	 * Evaluate the given expression against the given data sources and return 
	 * an object. If no source can resolve the symbol, null is returned.
	 * @param symbol the symbol to evaluate
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
	 * @param sourceString the original string
	 * @param srcs a set of data sources, evaluated in order 
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

	/**
	 * Interpret the given value as an integer. If the value is a string, evaluateSymbol is called and the result
	 * manipulated into an integer. If the value is an integer, the result is used directly. If processing fails, for
	 * any reason, the default value is returned.
	 * @param value
	 * @param defaultValue
	 * @param srcs
	 * @return an integer 
	 */
	public static int evaluateTextToInt ( Object value, int defaultValue, ExprDataSource... srcs )
	{
		if ( value == null ) return defaultValue;

		if ( value instanceof Integer ) return (Integer) value;

		final Object evalVal = evaluateText ( value.toString (), srcs );
		if ( evalVal == null ) return defaultValue;

		return TypeConvertor.convertToInt ( evalVal.toString (), defaultValue ); 
	}

	/**
	 * Interpret the given value as a boolean. If the value is a string, evaluateSymbol is called and the result
	 * manipulated into a boolean. If the value is a boolean, the result is used directly. If processing fails, for
	 * any reason, the default value is returned.
	 * @param value
	 * @param defaultValue
	 * @param srcs
	 * @return a boolean 
	 */
	public static boolean evaluateTextToBoolean ( Object value, boolean defaultValue, ExprDataSource... srcs )
	{
		if ( value == null ) return defaultValue;

		if ( value instanceof Boolean ) return (Boolean) value;

		final Object evalVal = evaluateText ( value.toString (), srcs );
		if ( evalVal == null ) return defaultValue;

		return TypeConvertor.convertToBooleanBroad ( evalVal.toString () ); 
	}

	
	private final ExprDataSource[] fSources;
}
