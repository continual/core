package io.continual.services.model.core.data;

import java.util.LinkedList;
import java.util.List;

public interface ModelList
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
	default ModelObject getMap ( int index )
	{
		return (ModelObject) get ( index );
	}

	/**
	 * Get a value as a list
	 * @param index
	 * @return a list value
	 */
	default ModelList getList ( int index )
	{
		return (ModelList) get ( index );
	}

	/**
	 * A visitor interface
	 * @param <T>
	 */
	interface Visitor<T>
	{
		void visit ( T t );
	}

	/**
	 * Visit each element
	 * @param <T>
	 * @param v
	 */
	@SuppressWarnings("unchecked")
	default <T> void forEach ( Visitor<T> v )
	{
		for ( int i=0; i<size(); i++ )
		{
			v.visit ( (T) get ( i ) );
		}
	}

	@SuppressWarnings("unchecked")
	default<T> List<T> listOf ( Class<T> clazz )
	{
		final LinkedList<T> result = new LinkedList<> ();
		forEach ( t -> result.add ( (T) t ) );
		return result;
	}

	/**
	 * Append a null value into the list
	 * @return this writer
	 */
	default ModelList addNull ()
	{
		return putNull ( size () );
	}

	/**
	 * Append a string to this list
	 * @param val
	 * @return this writer
	 */
	default ModelList add ( String val )
	{
		return put ( size (), val );
	}

	/**
	 * Append a number to this list
	 * @param val
	 * @return this writer
	 */
	default ModelList add ( Number val )
	{
		return put ( size (), val );
	}

	/**
	 * Append a boolean to this list
	 * @param val
	 * @return this writer
	 */
	default ModelList add ( boolean val )
	{
		return put ( size (), val );
	}

	/**
	 * Append an object to this list
	 * @param val
	 * @return this writer
	 */
	default ModelList add ( ModelObject val )
	{
		return put ( size (), val );
	}

	/**
	 * Append a list to this list
	 * @param val
	 * @return this writer
	 */
	default ModelList add ( ModelList val )
	{
		return put ( size (), val );
	}

	/**
	 * Put a null value into the list
	 * @param index
	 * @return this writer
	 */
	ModelList putNull ( int index );

	/**
	 * Put a string to this list
	 * @param index
	 * @param val
	 * @return this writer
	 */
	ModelList put ( int index, String val );

	/**
	 * Put a number to this list
	 * @param index
	 * @param val
	 * @return this writer
	 */
	ModelList put ( int index, Number val );

	/**
	 * Put a boolean to this list
	 * @param index
	 * @param val
	 * @return this writer
	 */
	ModelList put ( int index, boolean val );

	/**
	 * Append an object to this list
	 * @param index
	 * @param val
	 * @return this writer
	 */
	ModelList put ( int index, ModelObject val );

	/**
	 * Put a list to this list
	 * @param index
	 * @param val
	 * @return this writer
	 */
	ModelList put ( int index, ModelList val );
}
