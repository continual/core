package io.continual.services.model.core;

public interface ModelIdentification
{
	/**
	 * Get the ID of the account that owns this model.
	 * @return the account ID
	 */
	String getAcctId ();

	/**
	 * Get this model's name in the context of the containing account.
	 */
	String getId ();
}
