package io.continual.services.model.core.data;

/**
 * An adapter interface for reading data from an arbitrary object instance.
 */
public interface ModelDataListAccess
{
	/**
	 * Get the length of this list.
	 * @return this list's length
	 */
	int size ();

	/**
	 * Get a value generically. ModelDataNullValue.NULL indicates an explicit
	 * null value.
	 * @param index
	 * @return a value
	 */
	Object get ( int index );

	/**
	 * Get a value as a string
	 * @param index
	 * @return a string value
	 * @throws ClassCastException
	 */
	default String getString ( int index )
	{
		final Object val = get ( index );
		return val == null ? null : (String) val;
	}

	/**
	 * Get a value as a boolean
	 * @param index
	 * @return a boolean value
	 * @throws ClassCastException
	 */
	default boolean getBoolean ( int index )
	{
		return (Boolean) get ( index );
	}

	/**
	 * Get a value as a number
	 * @param index
	 * @return a number value
	 * @throws ClassCastException
	 */
	default Number getNumber ( int index )
	{
		return (Number) get ( index );
	}

	/**
	 * Get a value as a map
	 * @param index
	 * @return a map value
	 */
	default ModelDataObjectAccess getMap ( int index )
	{
		return (ModelDataObjectAccess) get ( index );
	}

	/**
	 * Get a value as a list
	 * @param index
	 * @return a list value
	 */
	default ModelDataListAccess getList ( int index )
	{
		return (ModelDataListAccess) get ( index );
	}

	/**
	 * Create an empty list.
	 * @return an empty list
	 */
	static ModelDataListAccess emptyList ()
	{
		return new ModelDataListAccess ()
		{
			@Override
			public int size () { return 0; }

			@Override
			public Object get ( int index ) { return null; }
		};
	}
}
