package io.continual.script;

import io.continual.metrics.MetricsSupplier;

/**
 * Evaluate a very lightweight scripting "language" against a set of bound data.
 */
public interface ScriptEvaluator<T> extends MetricsSupplier
{
	/**
	 * Register applicable functions from the given class. All methods of the form
	 * "static public String foo ( ScriptBindings, String[] )" will become available to
	 * the script engine. 
	 * @param clazz
	 * @return this evaluator
	 */
	ScriptEvaluator<T> registerFunctionsFrom ( Class<?> clazz );
	
	/**
	 * Evaluate the script as an expression and return a value
	 * @param bindings
	 * @return the expression value
	 * @throws ScriptSyntaxError
	 * @throws ScriptEvaluationException
	 */
	default T evaluateExpr ( ScriptBindings<T> bindings ) throws ScriptSyntaxError, ScriptEvaluationException
	{
		return evaluate ( bindings );
	}

	/**
	 * Evaluate the script as an expression and return a boolean value
	 * @param bindings
	 * @return true or false
	 * @throws ScriptSyntaxError
	 * @throws ScriptEvaluationException
	 */
	boolean evaluateExprAsBoolean ( ScriptBindings<T> bindings ) throws ScriptSyntaxError, ScriptEvaluationException;

	/**
	 * Execute the given script against the bindings.
	 * 
	 * @param epd
	 * @throws ScriptSyntaxError
	 * @throws ScriptEvaluationException
	 */
	T evaluate ( ScriptBindings<T> epd ) throws ScriptSyntaxError, ScriptEvaluationException;

	/**
	 * Evaluate the named function with the given arguments and bindings.
	 * @param name
	 * @param args
	 * @param epd
	 * @return
	 * @throws ScriptEvaluationException
	 */
	T evaluateFunction ( String name, T[] args, ScriptBindings<T> epd ) throws ScriptEvaluationException;
}
