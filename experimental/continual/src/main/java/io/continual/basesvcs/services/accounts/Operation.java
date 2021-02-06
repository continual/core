package io.continual.basesvcs.services.accounts;

/**
 * The set of authorized operations a user may take.
 * @author peter
 *
 */
public enum Operation
{
	/**
	 * Create a new item inside a container item
	 */
	CREATE,

	/**
	 * Read an item
	 */
	READ,

	/**
	 * Update an item
	 */
	UPDATE,

	/**
	 * Delete an item
	 */
	DELETE
}
