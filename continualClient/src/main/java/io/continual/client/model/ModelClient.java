package io.continual.client.model;

import java.io.IOException;
import java.util.List;

public interface ModelClient
{
	static class ModelServiceException extends Exception
	{
		public ModelServiceException ( String msg ) { super(msg); }
		public ModelServiceException ( Throwable t ) { super(t); }
		public ModelServiceException ( int statusCode, String msg ) { super(statusCode + ": " + msg); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Get the list of visible models for the given account.
	 * @param acctId
	 * @return a list of readable models
	 * @throws IOException 
	 * @throws ModelServiceException 
	 */
	List<String> getModels ( String acctId ) throws IOException, ModelServiceException;

	/**
	 * Get the JSON data for the given model
	 * @param acctId
	 * @param modelName
	 * @return a JSON data string
	 */
	String getModelData ( String acctId, String modelName );

	/**
	 * Create a model
	 * @param acctId
	 * @param modelName
	 */
	void createModel ( String acctId, String modelName );

	/**
	 * Delete a model
	 * @param acctId
	 * @param modelName
	 */
	void deleteModel ( String acctId, String modelName );

	/**
	 * Get an object from a model
	 * @param locator
	 * @return a model object
	 */
	ModelReference getObject ( ModelObjectLocator locator );

	/**
	 * Delete an object given a locator
	 * @param locator
	 */
	void deleteObject ( ModelObjectLocator locator );
}
