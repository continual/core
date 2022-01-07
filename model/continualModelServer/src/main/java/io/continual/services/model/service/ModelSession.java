package io.continual.services.model.service;

import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelNotificationService;
import io.continual.services.model.core.ModelSchemaRegistry;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.naming.Path;

/**
 * The Model session contains the set of mounted models available to a user.
 */
public interface ModelSession
{
	static class ModelPathDoesNotExistException extends ModelServiceException
	{
		public ModelPathDoesNotExistException ( Path p ) { super ( "Path does not exist: " + p.toString () ); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Get the top-level model for this session.
	 * @return a model
	 */
	Model getModel ();

	/**
	 * Get the schema registry
	 * @return a schema regsitry
	 */
	ModelSchemaRegistry getSchemaRegistry ();

	/**
	 * Get the notification service
	 * @return a notification service
	 */
	ModelNotificationService getNotificationSvc ();
}
