package io.continual.services.model.core.data;

import io.continual.services.model.core.ModelObjectMetadata;
import io.continual.util.naming.Path;

/**
 * A straightforward data access object.
 */
public class BasicModelObject
{
	public BasicModelObject ( Path p, ModelObjectMetadata meta, ModelObject data )
	{
		fMetadata = meta;
		fOrigData = data;
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
	public ModelObject getData ()
	{
		return fOrigData;
	}

	@Override
	public String toString ()
	{
		return getData().toString ();
	}

	private final ModelObjectMetadata fMetadata;
	private final ModelObject fOrigData;
}
