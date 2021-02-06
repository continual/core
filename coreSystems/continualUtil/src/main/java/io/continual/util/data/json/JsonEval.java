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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import io.continual.util.data.TypeConvertor;
import io.continual.util.data.exprEval.ExpressionEvaluator;
import io.continual.util.data.exprEval.JsonDataSource;

public class JsonEval
{
	/**
	 * Evaluate an expression against a JSON structure. If the expression refers to a non-existent
	 * node, null is returned.
	 *  
	 * @param root the root JSON document
	 * @param expression an expression to evaluate
	 * @return a JSON element or a primitive
	 */
	public static Object eval ( JSONObject root, String expression )
	{
		final String[] parts = expression.split ( "\\." );
		if ( parts.length == 1 )
		{
			return evalToValue ( root, parts[0] );
		}
		else
		{
			final JSONObject o = evalToContainer ( root, parts[0] );
			if ( o != null )
			{
				return eval ( o, expression.substring ( expression.indexOf ( '.' ) + 1 ) );
			}
		}
		return null;
	}

	/**
	 * Evaluate an expression against a JSON structure, using a default value if the referenced value
	 * doesn't exist.
	 * @param root the root JSON document
	 * @param expression an expression to evaluate
	 * @param defval the value to use if the expression evals to null
	 * @return a JSON value or the default provided
	 */
	public static String eval ( JSONObject root, String expression, String defval )
	{
		final Object val = eval ( root, expression );
		return val == null ? defval : val.toString ();
	}

	/**
	 * Evaluate the given expression against the given root JSON object and return 
	 * a string representation. If the evaluation is null, an empty string is returned.
	 * @param root the root JSON document
	 * @param expression an expression to evaluate
	 * @return a string
	 */
	public static String evalToString ( JSONObject root, String expression )
	{
		final Object result = eval ( root, expression );
		if ( result == null ) return "";
		return result.toString ();
	}
	
	/**
	 * Evaluate the given expression against the given root JSON object and return
	 * a boolean representation. If the evaluation is null, false is returned. If
	 * the evaluation results in a JSON boolean, the value is returned. Anything else
	 * is converted to a string (via toString) and converted to a boolean via
	 * rrConvertor.convertToBooleanBroad
	 * @param root the root JSON document
	 * @param expression an expression to evaluate
	 * @return true or false
	 */
	public static boolean evalToBoolean ( JSONObject root, String expression )
	{
		final Object result = eval ( root, expression );
		if ( result == null ) return false;
		if ( result instanceof Boolean )
		{
			return (Boolean) result;
		}
		return TypeConvertor.convertToBooleanBroad ( result.toString () );
	}

	/**
	 * Evaluate the given expression against the given root JSON object and return
	 * an integer representation. If the evaluation is null, the default value is returned.
	 * If the value is an integer, it's returned. Otherwise, the value is used as a string
	 * and then converted to an integer via rrConvertor.convertToInt with the default
	 * value as the default.
	 * 
	 * @param root the root JSON document
	 * @param expression an expression to evaluate
	 * @param defaultValue the default int value to use if the evaluation results in null
	 * @return an integer
	 */
	public static int evalToInt ( JSONObject root, String expression, int defaultValue )
	{
		final Object result = eval ( root, expression );
		if ( result == null ) return defaultValue;
		if ( result instanceof Integer )
		{
			return (Integer) result;
		}
		return TypeConvertor.convertToInt ( result.toString (), defaultValue );
	}

	/**
	 * Evaluate the given expression against the given root JSON object and return
	 * a double representation. If the evaluation is null, the default value is returned.
	 * If the value is a double, it's returned. Otherwise, the value is used as a string
	 * and then converted to a dboule via rrConvertor.convertToDouble with the default
	 * value as the default.
	 * 
	 * @param root the root JSON document
	 * @param expression an expression to evaluate
	 * @param defaultValue the default double value to use if the evaluation results in null
	 * @return a double
	 */
	public static double evalToDouble ( JSONObject root, String expression, double defaultValue )
	{
		final Object result = eval ( root, expression );
		if ( result == null ) return defaultValue;
		if ( result instanceof Number )
		{
			return ((Number) result).doubleValue();
		}
		return TypeConvertor.convertToDouble ( result.toString (), defaultValue );
	}

	/**
	 * Evaluate the given expression against the given root JSON object and return
	 * an object. If the evaluation is null, an empty object is returned. If the value
	 * is not an object, IllegalArgumentException is thrown
	 * 
	 * @param root the root JSON document
	 * @param expression an expression to evaluate
	 * @return an object, which is empty if none exists at the expression
	 */
	public static JSONObject evalToObject ( JSONObject root, String expression ) throws IllegalArgumentException
	{
		final Object result = eval ( root, expression );
		if ( result == null ) return new JSONObject ();
		if ( result instanceof JSONObject )
		{
			return (JSONObject) result;
		}
		throw new IllegalArgumentException ( expression + " is not an object." );
	}

	/**
	 * Evaluate the given expression against the given root JSON object and return
	 * an array. If the evaluation is null, an empty array is returned. If the value
	 * is not an array, IllegalArgumentException is thrown
	 * 
	 * @param root the root JSON document
	 * @param expression an expression to evaluate
	 * @return an object, which is empty if none exists at the expression
	 */
	public static JSONArray evalToArray ( JSONObject root, String expression ) throws IllegalArgumentException
	{
		final Object result = eval ( root, expression );
		if ( result == null ) return new JSONArray ();
		if ( result instanceof JSONArray )
		{
			return (JSONArray) result;
		}
		throw new IllegalArgumentException ( expression + " is not an array." );
	}

	/**
	 * substitute any occurrence of ${&lt;expr&gt;} with the evaluation of that expression 
	 * @param sourceString the initial string
	 * @param root the root JSON document
	 * @return a string
	 */
	public static String substitute ( String sourceString, JSONObject root )
	{
		return ExpressionEvaluator.evaluateText ( sourceString, new JsonDataSource ( root ) );
	}

	public static void setValue ( JSONObject root, String key, Object data )
	{
		setValue ( root, key, data, false );
	}

	public static void setValue ( JSONObject root, String key, Object data, boolean appendArray )
	{
		final String[] parts = key.split ( "\\." );
		final List<String> partList = new LinkedList<String> ( Arrays.asList ( parts ) );

		final String lastPart = partList.remove ( partList.size () - 1 );
		final JSONObject container = getContainer ( root, partList, true );
		
		if ( appendArray && container.has ( lastPart ) && container.get(lastPart) instanceof JSONArray )
		{
			container.getJSONArray ( lastPart ).put ( data );
		}
		else
		{
			container.put ( lastPart, data );
		}
	}

	public static boolean hasKey ( JSONObject root, String key )
	{
		try
		{
			final String[] parts = key.split ( "\\." );
			final List<String> partList = new LinkedList<String> ( Arrays.asList ( parts ) );

			final String lastPart = partList.remove ( partList.size () - 1 );
			final JSONObject container = getContainer ( root, partList, false );
			return container != null && container.has ( lastPart );
		}
		catch ( IllegalArgumentException e )
		{
			return false;
		}
	}

	public static JSONObject getContainer ( JSONObject root, String key )
	{
		final String[] parts = key.split ( "\\." );
		final List<String> partList = new LinkedList<String> ( Arrays.asList ( parts ) );
		return getContainer ( root, partList, false );
	}

	private static JSONObject getContainer ( JSONObject root, List<String> parts, boolean withCreate )
	{
		JSONObject current = root;
		for ( String thisPart : parts )
		{
			final Object nextContainer = current.opt ( thisPart );
			if ( nextContainer == null )
			{
				if ( withCreate )
				{
					// no such thing. create it as an object.
					final JSONObject newObj = new JSONObject ();
					current.put ( thisPart, newObj );
					current = newObj;
				}
				else
				{
					return null;
				}
			}
			else if ( nextContainer instanceof JSONObject )
			{
				// normal case
				current = (JSONObject) nextContainer;
			}
			else
			{
				// not a container we can support
				throw new IllegalArgumentException ( "Intermediate part " + thisPart + " is not an object." );
			}
		}
		return current;
	}

	private static Object termToArrayValue ( JSONObject root, String term )
	{
		final int openBrace = term.indexOf ( '[' );
		if ( openBrace > -1 && term.endsWith ( "]" ))		// note: foo[0[1]] would pass
		{
			final String key = term.substring ( 0, openBrace );
			final JSONArray a = root.optJSONArray ( key );
			if ( a != null )
			{
				try
				{
					final String indexStr = term.substring ( openBrace+1, term.length () - 1 );
					final int index = Integer.parseInt ( indexStr );
					return a.opt ( index );
				}
				catch ( NumberFormatException x )
				{
					return null;
				}
			}
		}
		return null;
	}

	private static Object evalToValue ( JSONObject root, String term )
	{
		final int openBrace = term.indexOf ( '[' );
		if ( openBrace > -1 && term.endsWith ( "]" ))		// note: foo[0[1]] would pass
		{
			return termToArrayValue ( root, term );
		}
		else
		{
			return root.opt ( term );
		}
	}

	private static JSONObject evalToContainer ( JSONObject root, String term )
	{
		final int openBrace = term.indexOf ( '[' );
		if ( openBrace > -1 && term.endsWith ( "]" ))		// note: foo[0[1]] would pass
		{
			final Object element = termToArrayValue ( root, term );
			if ( element instanceof JSONObject )
			{
				return (JSONObject) element;
			}
		}
		else
		{
			return root.optJSONObject ( term );
		}
		return null;
	}
}
