package io.continual.services.model.impl.json;

import io.continual.services.model.core.data.JsonModelObject;
import io.continual.services.model.core.data.ModelObject;

/**
 * In-memory data for an object.
 */
public class CommonObjectData extends JsonModelObject
{
	/**
	 * Construct an empty object
	 */
	public CommonObjectData ()
	{
		super ();
	}

	public CommonObjectData ( ModelObject data )
	{
		super ( JsonModelObject.modelObjectToJson ( data ) );
	}
}
