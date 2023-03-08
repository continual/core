package io.continual.script;

/**
 * Bindings for script evaluation
 */
public interface ScriptBindings<T>
{
	/**
	 * Return true if the bindings have a named value
	 * @param id
	 * @return true if the value exists
	 */
	default boolean hasValueFor ( String id )
	{
		return get ( id ) != null;
	}

	/**
	 * Get a value or return null if the key is not present. 
	 * @param id
	 * @return a value or null
	 */
	T get ( String id );

	/**
	 * Set a named value into the bindings.
	 * @param id
	 * @param val
	 */
	void set ( String id, T val ) throws ScriptEvaluationException;
}
