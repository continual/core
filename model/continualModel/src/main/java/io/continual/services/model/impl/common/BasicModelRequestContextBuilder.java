package io.continual.services.model.impl.common;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.identity.Identity;
import io.continual.services.model.core.Model.ModelRequestContextBuilder;
import io.continual.services.model.core.ModelNotificationService;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelSchema;
import io.continual.services.model.core.ModelSchemaRegistry;

public class BasicModelRequestContextBuilder implements ModelRequestContextBuilder
{
	public BasicModelRequestContextBuilder ()
	{
	}
	
	@Override
	public ModelRequestContextBuilder forUser ( Identity user )
	{
		fUser = user;
		return this;
	}

	@Override
	public ModelRequestContextBuilder withSchemasFrom ( ModelSchemaRegistry reg )
	{
		fSchemaReg = reg;
		return this;
	}

	@Override
	public ModelRequestContextBuilder withNotificationsTo ( ModelNotificationService notifications )
	{
		fNotificationSvc = notifications;
		return this;
	}

	@Override
	public ModelRequestContext build () throws BuildFailure
	{
		return new BasicModelRequestContext ( this );
	}

	Identity fUser = null;

	ModelSchemaRegistry fSchemaReg = new ModelSchemaRegistry () { @Override public ModelSchema getSchema ( String name ) { return null; } };

	ModelNotificationService fNotificationSvc = ModelNotificationService.noopNotifier ();
};
