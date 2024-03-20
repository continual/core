package io.continual.services.model.core.data;

import org.json.JSONArray;

public class JsonModelList implements ModelList
{
	public JsonModelList ()
	{
		fData = new JSONArray ();
	}

	JsonModelList ( JSONArray data )
	{
		fData = data;
	}

	/**
	 * Get the length of this list.
	 * @return this list's length
	 */
	@Override
	public int size ()
	{
		return fData.length ();
	}

	/**
	 * Get a value generically. ModelDataNullValue.NULL indicates an explicit
	 * null value.
	 * @param index
	 * @return a value
	 */
	@Override
	public Object get ( int index )
	{
		return JsonModelObject.jsonToIface ( fData.get ( index ) );
	}

	@Override
	public ModelList putNull ( int index )
	{
		return put ( index, ModelObjectNullValue.NULL );
	}

	@Override
	public ModelList put ( int index, String val )
	{
		return put ( index, val );
	}

	@Override
	public ModelList put ( int index, Number val )
	{
		return put ( index, val );
	}

	@Override
	public ModelList put ( int index, boolean val )
	{
		return put ( index, val );
	}

	@Override
	public ModelList put ( int index, ModelObject val )
	{
		return put ( index, val );
	}

	@Override
	public ModelList put ( int index, ModelList val )
	{
		return put ( index, val );
	}

	private final JSONArray fData;

	private ModelList put ( int index, Object val )
	{
		if ( index < 0 ) throw new IllegalArgumentException ( "List index is " + index );

		// grow the array to meet the index requested
		while ( index > size () )
		{
			putNull ( size () );
		}

		fData.put ( index, JsonModelObject.ifaceToJson ( val ) );

		return this;
	}
}
