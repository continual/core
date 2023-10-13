package io.continual.templating;

import java.util.Map;

public interface ContinualTemplateContext
{
	/**
	 * Get an object in the context by name.
	 * @param key
	 * @return an object, or null.
	 */
	Object get ( String key );
	
	/**
	 * Put an object into the render context with a name. 
	 * 
	 * @param key a non-null key
	 * @param o a non-null value
	 * @return this
	 */
	ContinualTemplateContext put ( String key, Object o );

	/**
	 * Put a set of key/value pairs into this context
	 * @param data
	 * @return this
	 */
	ContinualTemplateContext putAll ( Map<String, ?> data );

	/**
	 * Remove an object given its name.
	 * @param key a non-null key
	 * @return this
	 */
	ContinualTemplateContext remove ( String key );
}
