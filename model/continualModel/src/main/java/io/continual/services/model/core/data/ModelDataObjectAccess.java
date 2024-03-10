package io.continual.services.model.core.data;

import java.util.Set;
import java.util.TreeSet;

/**
 * An adapter interface for reading data from an arbitrary object instance.
 */
public interface ModelDataObjectAccess
{
	/**
	 * Get the keys in this object.
	 * @return a set of keys
	 */
	Set<String> getKeys ();

	/**
	 * Test for the existence of a key
	 * @param key
	 * @return true if the key is present
	 */
	default boolean hasKey ( String key )
	{
		return getKeys().contains ( key );
	}
	
	/**
	 * Get a value generically. ModelDataNullValue.NULL indicates an explicit
	 * null value. Java null indicates the key is not present.
	 * @param key
	 * @return a value, or null if the key is not present
	 */
	Object get ( String key );

	/**
	 * Get a value as a string
	 * @param key
	 * @return a string value
	 * @throws ClassCastException
	 */
	default String getString ( String key )
	{
		final Object val = get ( key );
		return val == null ? null : (String) val;
	}

	/**
	 * Get a value as a boolean
	 * @param key
	 * @return a boolean value
	 * @throws ClassCastException
	 */
	default boolean getBoolean ( String key )
	{
		return (Boolean) get ( key );
	}

	/**
	 * Get a value as a number
	 * @param key
	 * @return a number value
	 * @throws ClassCastException
	 */
	default Number getNumber ( String key )
	{
		return (Number) get ( key );
	}

	/**
	 * Get a value as a map
	 * @param key
	 * @return a map value
	 */
	default ModelDataObjectAccess getMap ( String key )
	{
		return (ModelDataObjectAccess) get ( key );
	}

	/**
	 * Get a value as a list
	 * @param key
	 * @return a list value
	 */
	default ModelDataListAccess getList ( String key )
	{
		return (ModelDataListAccess) get ( key );
	}

	/**
	 * Create an empty map.
	 * @return an empty map
	 */
	static ModelDataObjectAccess emptyMap ()
	{
		return new ModelDataObjectAccess ()
		{
			@Override
			public Set<String> getKeys () { return new TreeSet<> (); }

			@Override
			public Object get ( String key ) { return null; }
		};
	}
}
