package io.continual.services.model.core;

import io.continual.util.naming.Path;

/**
 * A listener for model updates
 */
public interface ModelNotificationService
{
	void onObjectCreate ( Path objectPath );

	void onObjectUpdate ( Path objectPath );

	void onObjectDelete ( Path objectPath );

	/**
	 * Create a no-op notifier
	 * @return a notification service
	 */
	static ModelNotificationService noopNotifier ()
	{
		return new ModelNotificationService ()
		{
			@Override
			public void onObjectCreate ( Path objectPath ) {}

			@Override
			public void onObjectUpdate ( Path objectPath ) {}

			@Override
			public void onObjectDelete ( Path objectPath ) {}
		};
	}
}
