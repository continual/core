package io.continual.client.model;

/**
 * The address of an object
 */
public class ModelObjectLocator
{
	public ModelObjectLocator ( String acctId, String model, String oid )
	{
		fAcctId = acctId;
		fModel = model;
		fOid = oid;
	}

	/**
	 * Get the account that holds the model
	 * @return an account ID
	 */
	public String getAcctId () { return fAcctId; }

	/**
	 * Get the model that holds the object
	 * @return a model name
	 */
	public String getModel () { return fModel; }

	/**
	 * Get the object ID in the model
	 * @return the object ID
	 */
	public String getOid () { return fOid; }

	private final String fAcctId;
	private final String fModel;
	private final String fOid;
}
