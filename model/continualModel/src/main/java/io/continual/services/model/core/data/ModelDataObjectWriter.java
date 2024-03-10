package io.continual.services.model.core.data;

public interface ModelDataObjectWriter extends ModelDataObjectAccess
{
	/**
	 * Clear this data object
	 */
	void clear ();

	/**
	 * Merge the given data into this data
	 * @param data
	 * @return this data object
	 */
	ModelDataObjectWriter merge ( ModelDataObjectAccess data );
}
