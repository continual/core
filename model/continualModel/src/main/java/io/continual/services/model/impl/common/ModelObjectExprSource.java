package io.continual.services.model.impl.common;

import io.continual.services.model.core.data.JsonModelList;
import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.data.ModelList;
import io.continual.services.model.core.data.ModelObject;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.exprEval.ExprDataSource;

/**
 * Use a Model data object as a data source for expression evaluation
 */
public class ModelObjectExprSource implements ExprDataSource
{
	public ModelObjectExprSource ( ModelObject obj )
	{
		fObj = obj;
	}

	@Override
	public Object eval ( String expression )
	{
		return eval ( fObj, expression );
	}

	/**
	 * Evaluate the given expression against the given root object and return 
	 * a string representation. If the evaluation is null, an empty string is returned.
	 * @param root the root object
	 * @param expression an expression to evaluate
	 * @return a string
	 */
	public static String evalToString ( ModelObject root, String expression )
	{
		final Object result = eval ( root, expression );
		if ( result == null ) return "";
		return result.toString ();
	}
	
	/**
	 * Evaluate the given expression against the given root object and return
	 * a boolean representation. If the evaluation is null, false is returned. If
	 * the evaluation results in a boolean, the value is returned. Anything else
	 * is converted to a string (via toString) and converted to a boolean via
	 * TypeConvertor.convertToBooleanBroad
	 * @param root the root object
	 * @param expression an expression to evaluate
	 * @return true or false
	 */
	public static boolean evalToBoolean ( ModelObject root, String expression )
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
	 * Evaluate the given expression against the given root object and return
	 * an integer representation. If the evaluation is null, the default value is returned.
	 * If the value is an integer, it's returned. Otherwise, the value is used as a string
	 * and then converted to an integer via TypeConvertor.convertToInt with the default
	 * value as the default.
	 * 
	 * @param root the root object
	 * @param expression an expression to evaluate
	 * @param defaultValue the default int value to use if the evaluation results in null
	 * @return an integer
	 */
	public static int evalToInt ( ModelObject root, String expression, int defaultValue )
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
	 * Evaluate the given expression against the given root object and return
	 * a long representation. If the evaluation is null, the default value is returned.
	 * If the value is a long, it's returned. Otherwise, the value is used as a string
	 * and then converted to a long via TypeConvertor.convertToLong with the default
	 * value as the default.
	 * 
	 * @param root the root object
	 * @param expression an expression to evaluate
	 * @param defaultValue the default int value to use if the evaluation results in null
	 * @return an integer
	 */
	public static long evalToLong ( ModelObject root, String expression, long defaultValue )
	{
		final Object result = eval ( root, expression );
		if ( result == null ) return defaultValue;
		if ( result instanceof Long )
		{
			return (Long) result;
		}
		return TypeConvertor.convertToLong ( result.toString (), defaultValue );
	}

	/**
	 * Evaluate the given expression against the given root object and return
	 * a double representation. If the evaluation is null, the default value is returned.
	 * If the value is a double, it's returned. Otherwise, the value is used as a string
	 * and then converted to a dboule via TypeConvertor.convertToDouble with the default
	 * value as the default.
	 * 
	 * @param root the root object
	 * @param expression an expression to evaluate
	 * @param defaultValue the default double value to use if the evaluation results in null
	 * @return a double
	 */
	public static double evalToDouble ( ModelObject root, String expression, double defaultValue )
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
	 * Evaluate the given expression against the given root object and return
	 * an object. If the evaluation is null, an empty object is returned. If the value
	 * is not an object, IllegalArgumentException is thrown
	 * 
	 * @param root the root object
	 * @param expression an expression to evaluate
	 * @return an object, which is empty if none exists at the expression
	 */
	public static ModelObject evalToObject ( ModelObject root, String expression ) throws IllegalArgumentException
	{
		final Object result = eval ( root, expression );
		if ( result == null ) return new JsonModelObject ();
		if ( result instanceof ModelObject )
		{
			return (ModelObject) result;
		}
		throw new IllegalArgumentException ( expression + " is not an object." );
	}

	/**
	 * Evaluate the given expression against the given root object and return
	 * a list. If the evaluation is null, an empty list is returned. If the value
	 * is not an array, IllegalArgumentException is thrown
	 * 
	 * @param root the root object
	 * @param expression an expression to evaluate
	 * @return a list, which is empty if none exists at the expression
	 */
	public static ModelList evalToArray ( ModelObject root, String expression ) throws IllegalArgumentException
	{
		final Object result = eval ( root, expression );
		if ( result == null ) return new JsonModelList ();
		if ( result instanceof ModelList )
		{
			return (ModelList) result;
		}
		throw new IllegalArgumentException ( expression + " is not a list." );
	}

	private static Object eval ( ModelObject root, String expression )
	{
		final String[] parts = expression.split ( "\\." );
		if ( parts.length == 1 )
		{
			return evalToValue ( root, parts[0] );
		}
		else
		{
			final ModelObject o = evalToContainer ( root, parts[0] );
			if ( o != null )
			{
				return eval ( o, expression.substring ( expression.indexOf ( '.' ) + 1 ) );
			}
		}
		return null;
	}

	private final ModelObject fObj;

	private static Object evalToValue ( ModelObject root, String term )
	{
		final int openBrace = term.indexOf ( '[' );
		if ( openBrace > -1 && term.endsWith ( "]" ))		// note: foo[0[1]] would pass
		{
			return termToArrayValue ( root, term );
		}
		else
		{
			return root.get ( term );
		}
	}

	private static ModelObject evalToContainer ( ModelObject root, String term )
	{
		final int openBrace = term.indexOf ( '[' );
		if ( openBrace > -1 && term.endsWith ( "]" ))		// note: foo[0[1]] would pass
		{
			final Object element = termToArrayValue ( root, term );
			if ( element instanceof ModelObject )
			{
				return (ModelObject) element;
			}
		}
		else
		{
			try
			{
				return root.getMap ( term );
			}
			catch ( ClassCastException x )
			{
				// ignore this and return null
				return null;
			}
		}
		return null;
	}

	private static Object termToArrayValue ( ModelObject root, String term )
	{
		final int openBrace = term.indexOf ( '[' );
		if ( openBrace > -1 && term.endsWith ( "]" ))		// note: foo[0[1]] would pass
		{
			final String key = term.substring ( 0, openBrace );
			try
			{
				final ModelList a = root.getList ( key );
				if ( a != null )
				{
					try
					{
						final String indexStr = term.substring ( openBrace+1, term.length () - 1 );
						final int index = Integer.parseInt ( indexStr );
						return a.get ( index );
					}
					catch ( NumberFormatException x )
					{
						return null;
					}
				}
			}
			catch ( ClassCastException x )
			{
				// ignore this and return null
				return null;
			}
		}
		return null;
	}
}
