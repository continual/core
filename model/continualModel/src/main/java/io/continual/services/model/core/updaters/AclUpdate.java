package io.continual.services.model.core.updaters;

import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelOperation;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelUpdater;

public class AclUpdate implements ModelUpdater
{
	public AclUpdate ()
	{
	}

	@Override
	public ModelOperation[] getAccessRequired ()
	{
		return new ModelOperation[] { ModelOperation.ACL_UPDATE };
	}

	@Override
	public void update ( ModelRequestContext context, ModelObject o )
	{
	}
}
