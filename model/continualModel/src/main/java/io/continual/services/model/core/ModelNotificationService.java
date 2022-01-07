package io.continual.services.model.core;

import io.continual.util.naming.Path;

public interface ModelNotificationService
{
	void onObjectCreate ( Path objectPath );

	void onObjectUpdate ( Path objectPath );

	void onObjectDelete ( Path objectPath );
}
