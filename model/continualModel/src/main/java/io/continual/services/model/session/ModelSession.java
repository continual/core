package io.continual.services.model.session;

import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelNotificationService;
import io.continual.services.model.core.ModelSchemaRegistry;

/**
 * The Model session contains the set of mounted models available to a user.
 */
public interface ModelSession
{
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
