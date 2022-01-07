package io.continual.services.model.impl.common;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.identity.Identity;
import io.continual.services.model.core.Model.ModelRequestContextBuilder;
import io.continual.services.model.core.ModelNotificationService;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelSchema;
import io.continual.services.model.core.ModelSchemaRegistry;
import io.continual.util.naming.Path;

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
	public ModelRequestContextBuilder mountedAt ( Path mountPoint )
	{
		fMountPoint = mountPoint;
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
	Path fMountPoint = Path.getRootPath ();

	ModelSchemaRegistry fSchemaReg = new ModelSchemaRegistry () { @Override public ModelSchema getSchema ( String name ) { return null; } };

	ModelNotificationService fNotificationSvc = new ModelNotificationService ()
	{
		@Override
		public void onObjectCreate ( Path objectPath ) {}

		@Override
		public void onObjectUpdate ( Path objectPath ) {}

		@Override
		public void onObjectDelete ( Path objectPath ) {}
	};
};
