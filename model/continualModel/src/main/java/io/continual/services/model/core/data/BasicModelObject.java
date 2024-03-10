package io.continual.services.model.core.data;

import org.json.JSONObject;

import io.continual.services.model.core.ModelObjectMetadata;
import io.continual.util.naming.Path;

/**
 * A straightforward data access object.
 */
public class BasicModelObject
{
	public BasicModelObject ( Path p, ModelObjectMetadata meta, ModelDataObjectAccess data )
	{
		fMetadata = meta;
		fOrigData = data;
		fData = null;
	}

	/**
	 * Get this object's metadata
	 * @return object metadata
	 */
	public ModelObjectMetadata getMetadata ()
	{
		return fMetadata;
	}

	/**
	 * Get a copy of the data in this object
	 * @return a JSON object
	 */
	public ModelDataObjectAccess getData ()
	{
		return fData == null ? fOrigData : fData;
	}

	/**
	 * Replace the data in this object with the given data.
	 * @param data
	 */
	public void putData ( ModelDataObjectAccess data )
	{
		prepDataWrite ();
		fData.clear ();
		patchData ( data );
	}

	/**
	 * Patch the data in this object with the given data. Pass an explicit null value
	 * to remove a key. Nested object values are evaluated as patches recursively (as long
	 * as the patch data and the object data both carry object values for a given key). Array
	 * values are overwritten.
	 * @param data
	 */
	public void patchData ( ModelDataObjectAccess data )
	{
		prepDataWrite ();
		fData.merge ( data );
	}

	@Override
	public String toString ()
	{
		return new JSONObject ()
			.put ( "data", ModelDataToJson.translate ( getData () ) )
			.put ( "meta", getMetadata().toJson () )
			.toString ()
		;
	}

	private final ModelObjectMetadata fMetadata;
	private final ModelDataObjectAccess fOrigData;
	private ModelDataObjectWriter fData;

	// copy on write
	private void prepDataWrite ()
	{
		if ( fData == null )
		{
			fData = new JsonObjectAccess ( ModelDataToJson.translate ( fOrigData ) );
		}
	}
}
