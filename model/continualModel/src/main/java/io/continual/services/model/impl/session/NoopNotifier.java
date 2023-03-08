package io.continual.services.model.impl.session;

import io.continual.services.model.core.ModelNotificationService;
import io.continual.util.naming.Path;

public class NoopNotifier
	implements ModelNotificationService
{

	@Override
	public void onObjectCreate ( Path objectPath )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onObjectUpdate ( Path objectPath )
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void onObjectDelete ( Path objectPath )
	{
		// TODO Auto-generated method stub

	}

}
