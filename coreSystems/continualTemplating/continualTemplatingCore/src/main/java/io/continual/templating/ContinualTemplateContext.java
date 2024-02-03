package io.continual.templating;

import java.util.Set;

public interface ContinualTemplateContext
{
	/**
	 * Get an object in the context by name.
	 * @param key
	 * @return an object, or null.
	 */
	Object get ( String key );

	/**
	 * Get all keys in this context
	 * @return a set of keys
	 */
	Set<String> keys ();
	
	/**
	 * Put an object into the render context with a name. 
	 * 
	 * @param key a non-null key
	 * @param o a non-null value
	 * @throws NullPointerException
	 */
	ContinualTemplateContext put ( String key, Object o );

	/**
	 * Remove an object given its name.
	 * @param key a non-null key
	 * @throws NullPointerException
	 */
	ContinualTemplateContext remove ( String key );
}
