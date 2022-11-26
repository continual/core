package io.continual.script;

import io.continual.metrics.MetricsSupplier;

/**
 * Evaluate a very lightweight scripting "language" against a set of bound data.
 */
public interface ScriptEvaluator extends MetricsSupplier
{
	/**
	 * Register applicable functions from the given class. All methods of the form
	 * "static public String foo ( ScriptBindings, String[] )" will become available to
	 * the script engine. 
	 * @param clazz
	 * @return this evaluator
	 */
	ScriptEvaluator registerFunctionsFrom ( Class<?> clazz );
	
	/**
	 * Evaluate the script as an expression and return a boolean value
	 * @param bindings
	 * @return the expression value
	 * @throws ScriptSyntaxError
	 * @throws ScriptEvaluationException
	 */
	String evaluateExpr ( ScriptBindings bindings ) throws ScriptSyntaxError, ScriptEvaluationException;

	/**
	 * Evaluate the script as an expression and return a boolean value
	 * @param bindings
	 * @return true or false
	 * @throws ScriptSyntaxError
	 * @throws ScriptEvaluationException
	 */
	boolean evaluateExprAsBoolean ( ScriptBindings bindings ) throws ScriptSyntaxError, ScriptEvaluationException;

	/**
	 * Execute the given script against the bindings.
	 * 
	 * @param epd
	 * @throws ScriptSyntaxError
	 * @throws ScriptEvaluationException
	 */
	String evaluate ( ScriptBindings epd ) throws ScriptSyntaxError, ScriptEvaluationException;

	String evaluateFunction ( String name, String[] args, ScriptBindings epd ) throws ScriptEvaluationException;
}
